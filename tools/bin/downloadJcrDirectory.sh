#!/bin/bash
# External tool for IntelliJ to download the contents of a directory via the Composum source servlet as a zip and unpack it,
# overriding the files of the selected directory.
# Arguments in "External Tool" entry: $FilePathRelativeToSourcepath$ , working dir: $FileDir$
# Optional arguments: host:port user:password protocol

# Optional: if you have installed 7z, the environment variable CPM_7Z_EXTRACT_EXCLUDE could be set to some wildcards of files you
# generally would not want to download, for example stuff that is generated for packages but is not a source:
# CPM_7Z_EXTRACT_EXCLUDE="-xr0!*.css -xr0!*.jar -xr0!*.min.js"

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
  CPM_HOST="localhost"
fi
if [ -z "$CPM_PORT" ]; then
  CPM_PORT="9090"
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

jcrpath="$1"
jcrpath="${jcrpath#jcr_root/}"
jcrpath="${jcrpath#root/}"
url="${CPM_PROTOCOL}://${CPM_HOST}:${CPM_PORT}/bin/cpm/nodes/source.zip/${jcrpath}"

echo "Arguments: $*"
echo "Dir: $(pwd)"
echo "Path: ${jcrpath}"
echo "URL: $url"

if [ -z $1 ]; then
  echo "NO SOURCE DIR GIVEN"
  exit 1
fi

TMPFIL="$(mktemp -u).zip"
trap "{ rm -f $TMPFIL; }" EXIT

curl -D - -s -S -o $TMPFIL -u $CPM_ADMINUSER:$CPM_ADMINPASSWD $url

ls -l $TMPFIL

echo
echo "FILES:"
echo

if command -v 7z &>/dev/null; then
  7z l $CPM_7Z_EXTRACT_EXCLUDE $TMPFIL
else
  unzip -l $TMPFIL
fi

echo
echo "EXTRACTION:"
echo
if command -v 7z &>/dev/null; then
  7z x "$CPM_CPM_7Z_EXTRACT_EXCLUDE" -y $TMPFIL
  if [ -n "$CPM_CPM_7Z_EXTRACT_EXCLUDE" ]; then
    echo Using 7z additional switches $CPM_7Z_EXTRACT_EXCLUDE
  fi
else
  unzip -o -u $TMPFIL
  echo
  echo "WARNING: 7z not found -> using unzip which might not support unicode."
fi

echo
echo DONE
echo
