#!/bin/bash
# External tool for IntelliJ to upload the contents of a directory via
# the Composum source update servlet
# Arguments in "External Tool" entry: $FilePathRelativeToSourcepath$

set -e

if [ -z "$CPM_HOST" ]; then
   CPM_HOST=localhost
fi

if [ -z "$CPM_PORT" ]; then
   CPM_PORT=9090
fi

if [ -z "$CPM_ADMINUSER" ]; then
   CPM_ADMINUSER=admin
fi

if [ -z "$CPM_ADMINPASSWD" ]; then
   CPM_ADMINPASSWD=admin
fi

echo Arguments "$*"
echo Dir: $(pwd)

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

TMPFIL=`mktemp -u`.zip
trap "{ rm -f $TMPFIL; }" EXIT

zip -r $TMPFIL $path

# the parameter :operation=updatetree currently serves no purpose but to sneakily prevent the Sling POST servlet to
# create a node at /bin/cpm/... when the servlet is present.

curl -u $CPM_ADMINUSER:$CPM_ADMINPASSWD -v -F "file=@$TMPFIL" http://$CPM_HOST:$CPM_PORT/bin/cpm/nodes/sourceuploadXX.zip
