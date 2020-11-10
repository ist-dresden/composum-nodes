This directory contains a set of shell scripts related to the Composum core.

- browserDownloadDirectory.sh: external tool for IntelliJ (and similar) to download a subtree from JCR in vault format
- browserUpdateDirectory.sh: external tool for IntelliJ (and similar) to update a subtree in JCR from a directory. The content is overwritten but metadata is kept as far as possible.

Example settings for using these as external tools in IntelliJ:
- browserDownloadDirectory.sh:  
Name: Download Directory  
Description: Downloads content corresponding to current directory from the JCR subtree as package and expands it into current directory  
Arguments: $FilePathRelativeToSourcePath$  
Working Directory: $FileDir$

- browserUpdateDirectory.sh:  
Name: Update Directory  
Description: Updates content corresponding to the current directory in the JCR subtree from the current directory  
Arguments: $FilePathRelativeToSourcePath$  
Working Directory: $FileDir$

If you want to specify a different host besides the default localhost:9090 with admin:admin, 
you can give additional arguments host:port user:password protocol to both scripts. 
These values can also be specified with environment variables CPM_HOST, CPM_PORT, CPM_ADMINUSER, CPM_PASSWORD, CPM_PROTOCOL .
