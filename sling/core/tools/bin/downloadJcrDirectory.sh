#!/bin/bash
# External tool for IntelliJ to download the contents of a directory via the Composum source servlet as a zip and unpack it,
# overriding the files of the selected directory.
# Arguments in "External Tool" entry: $FilePathRelativeToSourcepath$

set -e

if [ -n "$2" ]; then
  HOSTPORT=(${2//:/ })
  CPM_HOST=${HOSTPORT[0]}
  CPM_PORT=${HOSTPORT[1]}
fi

if [ -z "$CPM_HOST" ]; then
  CPM_HOST="localhost"
fi
if [ -z "$CPM_PORT" ]; then
  CPM_PORT="9090"
fi

if [ -z "$CPM_ADMINUSER" ]; then
   CPM_ADMINUSER=admin
fi

if [ -z "$CPM_ADMINPASSWD" ]; then
   CPM_ADMINPASSWD=admin
fi

echo Arguments "$*"
echo Dir: $(pwd)
echo URL: http://$CPM_HOST:$CPM_PORT/bin/cpm/nodes/source.zip/$1

if [ -z $1 ]; then
    echo NO SOURCE DIR GIVEN
    exit 1
fi

TMPFIL=`mktemp -u`.zip
trap "{ rm -f $TMPFIL; }" EXIT
#echo temporary file: $TMPFIL

curl -s -S -o $TMPFIL -u $CPM_ADMINUSER:$CPM_ADMINPASSWD http://$CPM_HOST:$CPM_PORT/bin/cpm/nodes/source.zip/$1

unzip -o -u $TMPFIL
