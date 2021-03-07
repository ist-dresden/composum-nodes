#!/bin/bash
#
# usage: install [<uri;'http://localhost:8080'>[ <scope;'all'>[ <srcpath;'.'>[ <user;'admin:admin']]]]
#

if [ -z "$1" ]; then URI="http://localhost:8080"; else URI="$1"; fi
SCOPE=""
if [ "$2" = "all" -o "$2" = "def" -o "$2" = "default" -o -z "$2" ]; then
  SCOPE="jslibs config commons console pckgmgr usermgr"
  fi
if [ "$2" = "console" -o "$2" = "min" -o "$2" = "minimum" ]; then
  SCOPE="jslibs config commons console"
  fi
if [ -z "${SCOPE}" ]; then SCOPE="$2"; fi
if [ -z "$3" ]; then SRC="."; else SRC="$3"; fi
if [ -z "$4" ]; then USER="admin:admin"; else USER="$4"; fi

echo "install to '${URI}':"
for key in ${SCOPE}; do
  for file in `ls ${SRC}/composum-nodes-*${key}* 2>/dev/null`; do
    if [ -r ${file} ]; then
      echo "  - ${file}"
      curl -u ${USER} -F action=install -F bundlestart=start -F bundlestartlevel=20 -F refreshPackages=refresh \
                      -F bundlefile=@"${file}" "${URI}/system/console/bundles"
      fi
    done
  done
echo "done."
