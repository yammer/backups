#!/bin/bash -e
set -o pipefail

# Backups service example script

# There are three main parts in this script
#   1. Configuration
#   2. Signaling a backup start to the backups service
#   3. Uploading your files

# If the script ends with an exit status of 0, the backup
# is automatically marked as successful on the service.
# If the script ends with an exit status != 0 it is marked as failed.
# Everything sent to stdout or stderr will be automatically captured
# and submitted to the service on completion.
#
# After calling backup::start, you can read the global variable ID if
# you want to know the current backup ID.
# After the backup is marked as completed on the server (even if that
# http request fails) a SIGUSR1 is sent to this script, so that you
# can trap it and do any cleanup.

# Load bash client libraries
. ../backup_functions.sh

# Local (script only) settings to override
BACKUP_ENDPOINT="https://localhost:8443"
AUTHORIZATION="CHANGEME"
SERVICE_NAME="example"

# Start the actual backup
echo "Starting backup for service ${SERVICE_NAME}"
backup::start

# Dump the data you want to backup and stream it to the service
echo "Dump started at `date`"
echo "My precious data" | backup::send - "dump.txt"
echo "Dump completed at `date`"

# Alternatively, dump to a file then upload that
# backup::send "/tmp/dump.txt"

# Done! A last API call will be made automatically at this point, submitting the script
# output and marking the backup as successful if we exited with 0.
