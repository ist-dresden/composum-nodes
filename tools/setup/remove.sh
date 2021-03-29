#!/bin/bash
#
# usage: remove [<uri;'http://localhost:8080'>[ <scope;'all'>[ <version;required!>[ <user;'admin:admin']]]
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
if [ -z "$4" ]; then USER="admin:admin"; else USER="$4"; fi
INSTPATH="/libs/composum/nodes/install"

echo "remove at '${URI}':"
for key in ${SCOPE}; do
  file="composum-nodes-${key}-$3.jar"
  bundle="com.composum.nodes.${key}"
  echo "  - ${file}"
  curl -u ${USER} -X DELETE "${URI}${INSTPATH}/${file}" 1>/dev/null 2>&1
  curl -u ${USER} -F action=uninstall "${URI}/system/console/bundles/${bundle}" 1>/dev/null 2>&1
  done
echo "done."
