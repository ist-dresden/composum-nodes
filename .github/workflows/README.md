# Composum Github Action Setup

This directory contains the setup for the Github Actions normally used in the Composum repositories.

(For IST users: See also the more extensive discussion 
[in the internal Composum documents](https://cloud.composum.com/content/ist/composum/home/internaldocumentatio/releasingWithGithub.html) 
with some non public detail.)

## Updating the workflows

The "master" of this directory is in composum-nodes - to keep maintenance simple, please change the files here and 
then copy the changes to the other repositories, but by using diff in the IDE or reviewing the changes before committing - 
there are some differences in some cases, especially in master.yml and in project composum-meta.

## Workflows and usage

### pullrequest.yml
As a sanity check, this workflow is triggered on every pull request. It does a build and test, but does not deploy.

### develop.yml
Does a build and test on every push to the develop branch. This also deploys to the IST testserver.

### master.yml
Does a build and test after something is merged to the master branch, and creates the site and deploys that to 
Github pages.

### setversion.yml
This workflow is triggered manually, and updates the version in the pom.xml files. Can be applied to every branch.

### createrelease.yml
This workflow is triggered manually, and creates a release on the branch it is triggered from. (Possibly you might 
want to use setversion.yml, if it's not the immediately next action). It is done in a way that failures shouldn't leave
any traces in the repository, so it can just be restarted after fixing the problem.

It's possible to do a dryRun. Please be aware that this does everything, including the upload to OSSRH, but does not
"release" the upload but drop it from there.
