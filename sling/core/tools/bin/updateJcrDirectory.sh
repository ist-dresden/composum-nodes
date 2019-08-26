#!/bin/bash
# External tool for IntelliJ to upload the contents of a directory via
# the Composum source update servlet
# Arguments in "External Tool" entry: $FilePathRelativeToSourcepath$ , working dir: $FileDir$
# Optional arguments: host:port user:password protocol

set -e

if [ -n "$2" ]; then
  HOSTPORT=(${2//:/ })
  CPM_HOST=${HOSTPORT[0]}
  CPM_PORT=${HOSTPORT[1]}
fi

if [ -n "$3" ]; then
  USERPASS=(${3//:/ })
  CPM_ADMINUSER=${USERPASS[0]}
  CPM_ADMINPASSWD=${USERPASS[1]}
fi

if [ -n "$4" ]; then
  CPM_PROTOCOL=($4)
fi

if [ -z "$CPM_HOST" ]; then
   CPM_HOST=localhost
fi

if [ -z "$CPM_PORT" ]; then
   CPM_PORT=9090
fi

if [ -z "$CPM_PROTOCOL" ]; then
   CPM_PROTOCOL=http
fi

if [ -z "$CPM_ADMINUSER" ]; then
   CPM_ADMINUSER=admin
fi

if [ -z "$CPM_ADMINPASSWD" ]; then
   CPM_ADMINPASSWD=admin
fi

dirname=`pwd`
rootdir=$dirname
path=`basename $dirname`
while [ `basename "$rootdir"` != "jcr_root" -a `dirname "$rootdir"` != "$rootdir" ]; do
    rootdir=`dirname $rootdir`;
    path=`basename $rootdir`/$path
done
rootdir=`dirname $rootdir`;
# echo $rootdir $path
cd $rootdir

echo Arguments "$*"
echo Dir: $(pwd)
echo URL: $CPM_PROTOCOL://$CPM_HOST:$CPM_PORT/bin/cpm/nodes/sourceupload.zip/${path#jcr_root/}

TMPFIL=`mktemp -u`.zip
trap "{ rm -f $TMPFIL; }" EXIT

zip -r $TMPFIL $path

# the parameter :operation=updatetree currently serves no purpose but to sneakily prevent the Sling POST servlet to
# create a node at /bin/cpm/... when the servlet is present.

curl -u $CPM_ADMINUSER:$CPM_ADMINPASSWD -v -F "file=@$TMPFIL" $CPM_PROTOCOL://$CPM_HOST:$CPM_PORT/bin/cpm/nodes/sourceupload.zip/${path#jcr_root/}
