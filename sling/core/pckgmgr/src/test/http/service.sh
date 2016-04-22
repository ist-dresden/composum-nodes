#!/bin/bash

if [ -n "$2" ]; then PORT="$2"; else PORT="4502"; fi
if [ -n "$3" ]; then HOST="$3"; else HOST="localhost"; fi
if [ -n "$4" ]; then CONTEXT="$4"; else CONTEXT=""; fi

curl -v -u "admin:admin" -F file=@"$1" "http://${HOST}:${PORT}${CONTEXT}/bin/core/package.service.html"
