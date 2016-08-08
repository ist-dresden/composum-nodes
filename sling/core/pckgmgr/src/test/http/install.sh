#!/bin/bash

if [ -n "$2" ]; then TYPE="$2"; else TYPE="html"; fi
if [ -n "$3" ]; then PORT="$3"; else PORT="4502"; fi
if [ -n "$4" ]; then HOST="$4"; else HOST="localhost"; fi
if [ -n "$5" ]; then CONTEXT="$5"; else CONTEXT=""; fi

CMD="curl -v -u admin:admin -X POST http://${HOST}:${PORT}${CONTEXT}/bin/cpm/package.install.${TYPE}$1"
echo "$CMD"
$CMD