#!/usr/bin/env bash
clear
# A little test script for manually trying out the SourceUpdateServlet.
# Caution: this relies on Composum running on localhost:9090 and that the private test content is checked out
# at $COMPOSUM/private and is installed.

# Try a (nonsensical) GET
# curl -i -u admin:admin -v "http://localhost:9090/bin/cpm/nodes/debug/sourceupload.xml/content/test/composum/pages/intermediate/site/home/subsite"

# create:
# cd /Users/hps/dev/composum/private/sling/prototype/test/src/main/content/jcr_root
# /bin/rm -f /Users/hps/Library/Preferences/IntelliJIdea2018.3/scratches/scratch.zip
# zip -r -9 /Users/hps/Library/Preferences/IntelliJIdea2018.3/scratches/scratch.zip content/test/composum/pages/intermediate/site

# Actual POST
curl -u admin:admin -v -F "file=@/Users/hps/Library/Preferences/IntelliJIdea2018.3/scratches/scratch.zip" "http://localhost:9090/bin/cpm/nodes/debug/sourceupload.zip/content/test/composum/pages/intermediate/site/home/subsite"

