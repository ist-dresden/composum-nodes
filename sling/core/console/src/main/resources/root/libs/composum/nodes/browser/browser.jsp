<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <html data-context-path="${slingRequest.contextPath}">
    <sling:call script="head.jsp"/>
    <body id="browser" class="console left-open top-open">
    <div id="ui">
        <sling:call script="dialogs.jsp"/>
        <sling:include resourceType="composum/nodes/console/components/navbar"/>
        <div id="content-wrapper">
            <div id="split-view-horizontal-split" class="split-pane horizontal-split fixed-left">
                <div class="split-pane-component left-pane">
                    <div id="browser-nav-split" class="split-pane vertical-split fixed-bottom favorites-closed">
                        <div class="split-pane-component top-pane">
                            <sling:include resourceType="composum/nodes/browser/components/favorites"/>
                        </div>
                        <div class="split-pane-divider"></div>
                        <div class="split-pane-component bottom-pane">
                            <div>
                                <div class="tree-panel">
                                    <div id="browser-tree" data-selected="${browser.path}">
                                    </div>
                                </div>
                                <div id="browser-tree-actions" class="tree-actions action-bar btn-toolbar"
                                     role="toolbar">
                                    <div class="align-left">
                                        <div class="menu btn-group btn-group-sm dropup" role="group">
                                            <button type="button"
                                                    class="glyphicon-menu-hamburger glyphicon btn btn-default dropdown-toggle"
                                                    data-toggle="dropdown" title="More actions..."><span class="label">More...</span>
                                            </button>
                                            <ul class="dropdown-menu" role="menu">
                                                <li><a href="#" class="checkout"
                                                       title="Checkout/Checkin the selected node">Checkout</a></li>
                                                <li><a href="#" class="lock"
                                                       title="Lock/Unlock the selected node">Lock</a></li>
                                                <li><a href="#" class="mixins"
                                                       title="View/Change the nodes mixin types">Mixin Types...</a></li>
                                                <li><a href="#" class="move" title="Move the selected node">Move
                                                    Node</a></li>
                                                <li><a href="#" class="rename"
                                                       title="Rename the selected node">Rename</a></li>
                                            </ul>
                                        </div>
                                        <div class="btn-group btn-group-sm" role="group">
                                            <button type="button"
                                                    class="create glyphicon-plus glyphicon btn btn-default"
                                                    title="Create a new node"><span class="label">Create</span></button>
                                            <button type="button"
                                                    class="delete glyphicon-minus glyphicon btn btn-default"
                                                    title="Delete selected node"><span class="label">Delete</span>
                                            </button>
                                        </div>
                                        <div class="btn-group btn-group-sm" role="group">
                                            <button type="button" class="copy fa fa-copy btn btn-default"
                                                    title="Copy selected node to clipboard"><span
                                                    class="label">Copy</span></button>
                                            <button type="button" class="paste fa fa-paste btn btn-default"
                                                    title="Paste node from clipboard into the selected node"><span
                                                    class="label">Paste</span></button>
                                        </div>
                                        <div class="btn-group btn-group-sm" role="group">
                                            <button type="button"
                                                    class="refresh glyphicon-refresh glyphicon btn btn-default"
                                                    title="Refresh the selected tree node"><span
                                                    class="label">Refresh</span></button>
                                        </div>
                                    </div>
                                    <div class="align-right">
                                        <div class="filter btn-group btn-group-sm dropup" role="group">
                                            <label class="filter"><span>default</span></label>
                                            <button type="button"
                                                    class="glyphicon-filter glyphicon btn btn-default dropdown-toggle"
                                                    data-toggle="dropdown" aria-expanded="false"
                                                    title="Filter for the tree"><span class="label">Filter</span>
                                            </button>
                                            <ul class="dropdown-menu" role="menu">
                                            </ul>
                                        </div>
                                        <div class="btn-group btn-group-sm" role="group">
                                            <button type="button" class="favorites fa btn btn-default"
                                                    title="Toogle favorites view on/off"><span
                                                    class="label">Favorites</span></button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="split-pane-divider split-pane-divider-main-horizontal"></div>
                <div class="split-pane-component right-pane">
                    <div id="split-view-vertical-split" class="split-pane vertical-split fixed-top">
                        <div class="split-pane-component top-pane query-split-pane">
                            <div id="browser-query">
                                <sling:call script="query.jsp"/>
                            </div>
                        </div>
                        <div class="split-pane-divider split-pane-divider-main-vertical"></div>
                        <div class="split-pane-component bottom-pane">
                            <div id="browser-view">
                                <sling:call script="view.jsp"/>
                            </div>
                            <div class="close-top"><a href="#" class="fa fa-angle-double-up"
                                                      title="Collapse top panel"></a></div>
                        </div>
                        <div class="open-top"><a href="#" class="fa fa-angle-double-down" title="Restore top panel"></a>
                        </div>
                    </div>
                    <div class="close-left"><a href="#" class="fa fa-angle-double-left" title="Collapse left panel"></a>
                    </div>
                </div>
                <div class="open-left"><a href="#" class="fa fa-angle-double-right" title="Restore left panel"></a>
                </div>
            </div>
        </div>
    </div>
    <sling:call script="script.jsp"/>
    <sling:include resourceType="composum/nodes/console/components/tryLogin"/>
    </body>
    </html>
</cpn:component>
