# Tipps how to create a new browser view

- The corresponding components are here: console/src/main/resources/root/libs/composum/nodes/browser/components . references is the last component that is best used as a reference.
- In console/src/main/resources/root/libs/composum/nodes/browser/views/std/tabs.jsp the new view is added to the tabs.
- console/src/main/resources/root/libs/composum/nodes/browser/tabs needs an entry for the new view, which just should include the component (compare references.jsp)

## Dialog registration

Registration of the clientlib in console/src/main/resources/root/libs/composum/nodes/browser/clientlibs.json .
Registration of JS entry point in nodeview.js browser.detailViewTabTypes .
