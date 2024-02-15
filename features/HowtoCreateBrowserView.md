# Tipps how to create a new browser view

## Basic paths

- The corresponding components are here: console/src/main/resources/root/libs/composum/nodes/browser/components .
  references is the last component that is best used as a reference.
- In console/src/main/resources/root/libs/composum/nodes/browser/views/std/tabs.jsp the new view is added to the tabs.
- console/src/main/resources/root/libs/composum/nodes/browser/tabs needs an entry for the new view, which just should
  include the component (compare references.jsp)
- in package/sling/src/main/content/jcr_root/libs/composum/nodes.setup/browser/components add the .content.xml according
  to the new console/src/main/resources/root/libs/composum/nodes/browser/components/*.json in case it's installed via
  package

## Tab registration

Registration of the clientlib in console/src/main/resources/root/libs/composum/nodes/browser/clientlibs.json as well as
package/sling/src/main/content/jcr_root/libs/composum/nodes.setup/browser/clientlibs/browser/.content.xml
Registration of JS entry point in nodeview.js browser.detailViewTabTypes .

## General hints

The toolbar is loaded initially (toolbar.jsp), the actual content content is loaded on showing the tab into a
div.detail-content to reduce the load.

It's advisable to consider loading the actual data of the view via JSON. That might save some nightmares with encoding.

## Loaded dialogs

Example: references.jsp openOptions
