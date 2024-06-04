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

dirname=$(pwd)
rootdir="$dirname"
path=$(basename $dirname)
while [ $(basename "$rootdir") != "jcr_root" -a $(basename "$rootdir") != "root" -a $(dirname "$rootdir") != "$rootdir" ]; do
  rootdir=$(dirname $rootdir)
  path=$(basename $rootdir)/$path
done
rootdir=$(dirname $rootdir)

jcrpath="${path#src/main/content/jcr_root/}"
jcrpath="${jcrpath#src/main/resources/root/}"
jcrpath="${jcrpath#jcr_root/}"
jcrpath="${jcrpath#root/}"
url="$CPM_PROTOCOL://$CPM_HOST:$CPM_PORT/bin/cpm/nodes/sourceupload.zip/${jcrpath}"

echo "Arguments: $*"
echo "Dir: $(pwd)"
echo "Root: $rootdir"
echo "Path: $jcrpath"
echo "URL: $url"

TMPFIL=$(mktemp -u).zip
TMPDIR=$(mktemp -d)
trap "{ rm -f $TMPFIL; rm -fr $TMPDIR; }" EXIT

mkdir -p $TMPDIR/jcr_root/$jcrpath
cp -cR $(pwd) $(dirname $TMPDIR/jcr_root/$jcrpath/)

cd $TMPDIR
if command -v 7z &>/dev/null; then
  7z a $TMPFIL jcr_root/$jcrpath
  7z l $TMPFIL
else
  zip -vr $TMPFIL jcr_root/$jcrpath
  echo
  echo "WARNING: 7z not found -> using zip which might not support unicode."
fi

# the parameter :operation=updatetree currently serves no purpose but to sneakily prevent the Sling POST servlet to
# create a node at /bin/cpm/... when the servlet is present.

curl -u $CPM_ADMINUSER:$CPM_ADMINPASSWD -D - -F "file=@$TMPFIL" $url

echo "$(basename $0) uploaded /$jcrpath"
