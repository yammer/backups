# Bash backup client library
#
# This library is intended to be sourced in your custom backup client script.
# See the provided example backup and verification scripts in this directory
# for more details
#

# Read only constants
declare -r _BACKUP_CLIENT_LIB_VERSION="0.8"
declare -r _BACKUP_LOG_FILE=`mktemp -t backup.XXXXX`
declare -r _AUTH_SCHEMA="Token"
declare -r _SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
declare -r _BACKUP_PID=$$

# Global settings. These can be overriden in your own script.

# Number of retries on failure
BACKUP_RETRIES=${BACKUP_RETRIES:-3}

# Number of seconds to sleep before next retry
RETRY_SLEEP=${RETRY_SLEEP:-300}

# Directory where to put the PID file (for locking purposes)
PID_DIR=${PID_DIR:-'/tmp'}

# Name of the service being backed up
SERVICE_NAME=${SERVICE_NAME:-$HOSTNAME}

# Authorization header
AUTHORIZATION=${AUTHORIZATION:-""}

# Curl options
CURL_OPTS=${CURL_OPTS:-"-s -S -f"}

# Soft timeout. SIGTERM will be sent to the script after these seconds
BACKUP_TIMEOUT=${BACKUP_TIMEOUT:-82800} #23hs

# Hard timeout. The script will be killed with SIGKILL after these seconds
BACKUP_HARD_TIMEOUT=${BACKUP_HARD_TIMEOUT:-84600} #23:30hs

# global ID, to be used by the client script if needed
ID=

# global ID of the latest backup, to be used by the client script if needed
LATEST_BACKUP_ID=

# The URL of the Backups service
BACKUP_ENDPOINT=${BACKUP_ENDPOINT:-""}

# Private varialbes
# Current backup attempt
_BACKUP_RETRY_COUNT=${_BACKUP_RETRY_COUNT:-1}

# Current operation, one of 'backup' or 'verification'
_OPERATION=
# Public client API

# backup::start will signal the start of a new backup in the service. On success,
# the current backup id is stored in the global ID variable. Otherwise the execution
# is terminated
function backup::start {
    _OPERATION="backup"
    backup::_start
}

# backup::verification::start is the equivalent of backup::start, but for verifications
function backup::verification::start {
    _OPERATION="verification"
    backup::_latest_id
    backup::_start "backupId=${LATEST_BACKUP_ID}"
}

# backup::get download from the server the file $2 from the backup id $1 into the file $3.
# if $3 is a directory, a file with the server side provided name is created in it
# *IMPORTANT*: As this function can be used in a pipe, do not echo anything unless you will
# exit with a value != 0
function backup::get {
    if [ $# -lt 3 ];then
        echo "Requested backup download without providing a filename, backup ID, service and/or target dir"
        exit 1
    fi
    local id=$1
    local filename=$2
    local target=$3

    if [ -d "${target}" ];then
        target="${target}/${filename}"
    fi

    local url="${BACKUP_ENDPOINT}/api/backup/${SERVICE_NAME}/${id}/${filename}"
    curl -H "Authorization: ${_AUTH_SCHEMA} ${AUTHORIZATION}" ${CURL_OPTS} -o ${target} ${url}
}

# backup::verification::get downloads the 'latest' succesfull backup of the file $1 into $2
# *IMPORTANT*: As this function can be used in a pipe, do not echo anything unless you will
# exit with a value != 0
function backup::verification::get {
    if [ -z "${ID}" ];then
        echo "ERROR: calling backup::verification::get without an ID (Did you call backup::verification::start?)"
        exit 1
    fi
    if [ $# -lt 2 ]; then
        echo "backup::verification::get called without enough arguments"
        exit 1
    fi
    backup::get ${LATEST_BACKUP_ID} $1 $2
}


# backup::send will upload to the server the file provided as $1, the program will be
# terminated and an attempt to mark the backup as failed will be done if an error occurs.
function backup::send {
    if [ -z "${ID}"  ];then
        echo "ERROR: Calling send file without having an ID (Did you call backup::start?)"
        exit 1
    fi
    if [ $# -lt 1 ]; then
        echo "backup::send called without any argument"
        exit 1
    fi
    local filesource
    local filename

    filesource=$1

    if [ $# -ge 2 ]; then
        filename=$2
    else
        if [ "${filesource}" == "-" ];then
            echo "No filename provided for stdin provided data"
            exit 1
        else
            filename=`basename ${filesource}`
        fi
    fi

    if [ "${filesource}" != "-" ] && [ ! -f ${filesource} ];then
        echo "File ${filesource} not found"
        exit 1
    fi

    echo "Starting upload of file ${filename}"

    output=`cat ${filesource} | curl -H "Authorization: ${_AUTH_SCHEMA} ${AUTHORIZATION}" ${CURL_OPTS} -X PUT -H "Content-Type: application/x-www-form-urlencoded" ${BACKUP_ENDPOINT}/api/backup/${SERVICE_NAME}/${ID}/${filename} -T - `
    if [ $? -ne 0 ];then
        echo "Error uploading file: ${output}"
        exit 1
    else
        echo "Completed upload of ${filesource} as ${filename}"
    fi

}

# Private functions start here. Don't use in client scripts.


# backup::_start signals the beginning of an operation tot the server
function backup::_start {
    QUERY_PARAMS=$1

    # Log the backup client api in use
    echo "Backup client API ${_BACKUP_CLIENT_LIB_VERSION} starting ${_OPERATION} operation on `hostname -f`"

    # Lock if possible
    backups::_flock_if_available "${PID_DIR}/${SERVICE_NAME}.${_OPERATION}"
    trap 'backup::_complete ${_OPERATION} $? ${FUNCNAME}' EXIT

    # Ensure we have an endpoint
    if [ "${BACKUP_ENDPOINT}" == "" ]; then
        echo "Backups endpoint settings not present. Aborting".
        exit 1
    fi

    # Ensure we have the right credentials
    if [ "${AUTHORIZATION}" == "" ]; then
        echo "Authorization settings not present. Aborting".
        exit 1
    fi

    if [ `(echo ${#AUTHORIZATION})` -lt 10 ]; then
        echo "The authorization token must be at least 10 chars long"
        exit 1
    fi

    echo "${_OPERATION} attempt ${_BACKUP_RETRY_COUNT} of ${BACKUP_RETRIES}"

    echo "Signaling start of ${_OPERATION} for ${SERVICE_NAME} to endpoint ${BACKUP_ENDPOINT}"
    output=`curl -H "Authorization: ${_AUTH_SCHEMA} ${AUTHORIZATION}" ${CURL_OPTS} -X POST ${BACKUP_ENDPOINT}/api/${_OPERATION}/${SERVICE_NAME}?${QUERY_PARAMS}`
    ID=${output}
    echo "${_OPERATION} ${ID} started."

    # Setup timeouts
    echo "Setting up soft timeout watchdog to ${BACKUP_TIMEOUT}"
    (sleep ${BACKUP_TIMEOUT};echo "WARNING: soft timeout reached at ${BACKUP_TIMEOUT}. Sending SIGTERM to ${_BACKUP_PID}";kill ${_BACKUP_PID}) &
    _backup_timeout_pid=$!

    echo "Setting up hard timeout watchdog to ${BACKUP_HARD_TIMEOUT}"
    (sleep ${BACKUP_HARD_TIMEOUT};echo "WARNING: hard timeout reached at ${BACKUP_HARD_TIMEOUT}.Sending SIGKILL to ${_BACKUP_PID}";backup::_finish 1;kill -9 ${_BACKUP_PID}) &
    _backup_hard_timeout_pid=$!
}



function backups::_timestamp_tee {
    while read input
    do
        echo "`date -u`: ${input}" | tee -a ${_BACKUP_LOG_FILE}
        echo "${input}" | logger -t "backups"
    done
}

function backups::_flock_if_available {
    if [ -x "`which flock`" ];then
        echo "Locking $1 to prevent parallel runs"

        # Ensure writable by any user
        if [ ! -e $1 ]; then
            touch $1
            chmod 777 $1
        fi

        exec 200>$1
        flock -n 200
        if [ $? -ne 0 ]; then
            echo "Another instance seems to be running. Aborting."
            exit 1
        fi
        echo $$ 1>&200
    fi
}

function backups::_unflock {
    if [ -x "`which flock`" ];then
        echo "Releasing lock"
        flock -u 200
    fi
}

function backup::_latest_id {
    echo "Retrieving latest backup metadata"
    local metadata
    metadata=`curl -H "Authorization: ${_AUTH_SCHEMA} ${AUTHORIZATION}" ${CURL_OPTS} -X GET ${BACKUP_ENDPOINT}/api/backup/${SERVICE_NAME}/latest`

    # Ensure the latest backup didn't fail
    local state
    state=`echo "${metadata}" | jq -r .state`

    # Set global LATEST_BACKUP_ID
    LATEST_BACKUP_ID=`echo "${metadata}" | jq -r .id`
    if [ "${state}" == "FAILED" ] || [ "${state}" == "TIMEDOUT" ];then
        echo "Can't run verification because the latest backup (${LATEST_BACKUP_ID}) status is ${state}"
        exit 1
    fi

    echo "Latest available backup for verification is ${LATEST_BACKUP_ID}"
}


# backup::_finish posts the logs and mark the backup/verification as completed
# $1 must be "true" if the operation succeeded, "false" otherwise

function backup::_finish {
    local success
    success=$1

    echo "Sending finish signal to server and submitting logs"
    sleep 1 # Without this sleep the last few lines appended to the log are not submitted to the server.
    curl -H "Authorization: ${_AUTH_SCHEMA} ${AUTHORIZATION}" ${CURL_OPTS} -X POST ${BACKUP_ENDPOINT}/api/${_OPERATION}/${SERVICE_NAME}/${ID}/finish?success=${success} --data-binary @${_BACKUP_LOG_FILE}
    rm -f ${_BACKUP_LOG_FILE}
}

# backup::_complete will mark as completed the current backup. Program terminates on error.
# $1 is the entity being completed: either 'backup' or 'verification'
# $2 is the exit status ( 0 = success, anything else is a failure)
# $3 is the function name where termination was requested
function backup::_complete {
    if [ "$3" != "" ]; then
        echo "Process request exit in $3"
    fi

    # Give the client script the opportunity to clean up
    echo "Sending USR1 signal to current process ($$)"
    kill -USR1 $$ >/dev/null

    echo "Script exit status is $2, on backup attempt ${_BACKUP_RETRY_COUNT} of ${BACKUP_RETRIES}"
    local success
    if [ $2 -eq 0 ];then
        echo "SUCCESS"
        success="true"
    else
        echo "FAILED"
        success="false"
    fi

    # Notify the completion to the server
    set +e
    backup::_finish ${success}

    # Kill timeout watchdogs
    pkill -9 -P ${_backup_timeout_pid} > /dev/null 2>&1
    pkill -9 -P ${_backup_hard_timeout_pid} > /dev/null 2>&1

    # If it failed and we have more retries, re-run.
    if [ $2 -ne 0 ] && [ ${_BACKUP_RETRY_COUNT} -lt ${BACKUP_RETRIES} ]; then
        _BACKUP_RETRY_COUNT=$((_BACKUP_RETRY_COUNT+1))
        export _BACKUP_RETRY_COUNT
        # Rebuild command line arguments
        local args
        args=""
        for (( i=0 ; i<${BASH_ARGC:-0} ; i++ ));do
            args="${BASH_ARGV[i]} $args"
        done

        # Wait for next retry.
        echo "Sleeping for ${RETRY_SLEEP} before next retry."
        sleep ${RETRY_SLEEP}

        # Restore stdout
        exec 1<&6 6<&-

        # Release lock so that new instance can run
        backups::_unflock

        # Replace current process with a new one for retrying
        exec $0 ${args}
    fi

    exit
}

# Main

# Cancel backup on SIGINT, SIGTERM or SIGQUIT
trap 'exit 1' SIGINT SIGTERM SIGQUIT
trap '' USR1

# Save stdout for later restoration
exec 6<&1
# Capture all output

exec 1> >(backups::_timestamp_tee)
exec 2>&1


