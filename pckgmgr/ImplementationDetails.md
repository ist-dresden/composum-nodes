# Some implementation details

Some notes that may or may not help a developer when improving the package manager. :-)

## Requests for a package manager view, example and rendering structure
- /bin/cpm/package/registryTree.json/path  -> Update the tree

- /bin/packages.view.html/path : right frame, div.detail-content = replaced with Ajax later  
  - type=composum/nodes/pckgmgr selectorString='view' /libs/composum/nodes/pckgmgr/view.jsp
    - type=composum/nodes/pckgmgr/version selectorString='regpckg' /libs/composum/nodes/pckgmgr/version/regpckg.jsp

- /bin/packages.tab.general.html/path : content for the package/version/etc. , 
  - type=composum/nodes/pckgmgr selectorString='tab.general' /libs/composum/nodes/pckgmgr/tab.jsp
    - type=composum/nodes/pckgmgr/version/general selectorString='regpckg' /libs/composum/nodes/pckgmgr/version/general/general.jsp
    - type=composum/nodes/pckgmgr/version/general selectorString='header' /libs/composum/nodes/pckgmgr/version/general/header.jsp
    - type=composum/nodes/pckgmgr/version/general selectorString='summary' /libs/composum/nodes/pckgmgr/version/general/summary.jsp
    - type=composum/nodes/pckgmgr/version/general selectorString='status' /libs/composum/nodes/pckgmgr/version/general/status.jsp

- /bin/packages.header.html/path
  - type=composum/nodes/pckgmgr /libs/composum/nodes/pckgmgr/header.jsp
    - type=composum/nodes/pckgmgr/version/general /libs/composum/nodes/pckgmgr/version/general/header.jsp
    - type=composum/nodes/pckgmgr/version/general /libs/composum/nodes/pckgmgr/version/general/summary.jsp
    - type=composum/nodes/pckgmgr/version/general /libs/composum/nodes/pckgmgr/version/general/status.jsp

## divs structure for the general package manager view structure
- div.detail-tabs.action-bar : tabs
- div.display-toolbar.detail-toolbar : actions on displayed node
- div.package-detail : detail pane
  - div.header-view : general information
  - div.detail-view : detailed information

## Example: package list at jcrpckg
/bin/packages.view.html/path : frame with detail-view > detail-panel group > (detail-tabs + detail-content (leer))
/bin/packages.tab.general.html/path : the actual list of packages.
  group/general.jcrpckg = group/general/general.jsp -> explicitly calls jcrpckg/general.listitem . includes status.jsp

## Component structure of the package manager

Base path: /libs/composum/nodes/pckgmgr/ = base component with selectors for the basic areas of the component:
head.jsp = HTML header , script.jsp: includes the necessary javascript parts , query.jsp: query frame
header.jsp: forward to ${pkgmgr.viewType}/general header , 
pckgmgr.jsp: general HTML structure (tree.jsp, query.jsp, view.jsp), parts are reloaded with AJAX.
view.jsp: general frame for the part on the right (below query) , forward to ${pckgmgr.viewType} ${pckgmgr.mode} 
  (also for AJAX calls)
tab.jsp: forward to ${pckgmgr.viewType}/${pckgmgr.tabType} ${pckgmgr.mode} (for AJAX calls)

Basic components for ${pckgmgr.viewType} : packages, registry, group, jcrpckg, regpckg, version
subcomponents with ${pckgmgr.tabType} : general, filter, content, options
/bin/packages.view.html/path : selector ${pckgmgr.mode} : jcrpckg, regpckg

## Models
com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean
vs.
com.composum.sling.core.pckgmgr.regpckg.view.PackageBean

## Icons
find them in https://fontawesome.com/v4/icons/ or https://glyphicons.bootstrapcheatsheets.com/ .

cleanup of old versions: eraser? recycle?

## CSS Struktur der views:
### JCR Package Manager Detailviews 
div.pckgmgr-view div.detail-view ... 
#### Gruppe:
div.detail-panel.group div.detail-content div.detail-panel.group div.group-detail 
  e.g. div.pckg-list-item.panel.panel-default ...
#### Package:
div.detail-panel.jcrpckg div.detail-content div.detail-panel.package div.package-detail
### Registry Package Manager: div.pckgmgr-view div.detail-view ...
#### Gruppe:
div.detail-panel.group div.detail-content div.detail-panel.group div.group-detail
#### Package:
div.detail-panel.jcrpckg div.detail-content div.detail-panel.package div.package-detail
#### Version:
div.detail-panel.jcrpckg div.detail-content div.detail-panel.package div.package-detail

## Package cleanup implementation plan
Triggered at tree: a dialog presents all probably obsolete package versions as checkbox list, pre-selected.
(Buttons for selecting all and deselecting all versions are present).
AJAX Request to /bin/packages.cleanupoptions.html gives selectable options for packages.

Request deleting the versions: POST request to /bin/cpm/package.json, parameter path=/@jcr/hpsgroup/hpsx/1.4asdf 
for all versions. Problem: the list of versions can be very long, and 
https://www.mscharhag.com/api-design/rest-deleting-resources recommends not using a body... Unclear yet. POST?

## Tips and tricks
To let IntelliJ know what type a JSP variable is (since it can't tell from the Composum tags), you can add a comment 
e.g. like this:
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean" scope="request">
    <%--@elvariable id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean"--%>

To find out easier what JSPs were used for the rendering, these settings might be useful:
Composum Nodes Clientlib Configuration: debug; minimize css off
Composum Nodes Debugutil Renderinfo Comment Logging Filter: url regex .*packa.* , extension regex htm.* (from nodes/xtracts/debugutil)
