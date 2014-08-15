#!/bin/bash -e
set -o pipefail

# Backups service verification script example

# There are three main parts in this script
#   1. Configuration
#   2. Signaling a verification start to the backups service
#   3. Downloading and verifying your files

# If the script ends with an exit status of 0, the verification
# is automatically marked as successful on the service.
# If the script ends with an exit status != 0 it is marked as failed.
# Everything sent to stdout or stderr will be automatically captured
# and submitted to the service on completion.
#
# After calling backups::verification::start, you can read
# the global variable ID if you want to know the current backup ID.
# After the verification is marked as completed on the server
# (even if that http request fails) a SIGUSR1 is sent to this script,
# so that you can trap it and do any cleanup.


# Load bash client libraries
. ../backup_functions.sh

# Local (script only) settings to override
BACKUP_ENDPOINT="https://localhost:8443"
AUTHORIZATION="CHANGEME"
SERVICE_NAME="example"

# Let the service know that we are starting a verification
echo "Starting verification for service ${SERVICE_NAME}"
backup::verification::start

# Download the latest successfull backup
echo "Verification started at `date`"
backup::verification::get "dump.txt" - | grep -H "My precious data"
echo "Verification completed at `date`"

# Alternatively, dump to a file then verify that
# backup::verification::get "dump.txt" "/tmp/dump.txt"
# grep -H "My precious data" "/tmp/dump.txt"

# Done! A last API call will be made automatically at this point, submitting the script
# output and marking the verification as successful if we exited with 0.
