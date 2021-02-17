<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
    <html data-context-path="${slingRequest.contextPath}">
    <sling:call script="head.jsp"/>
    <body id="pckgmgr" class="console left-open top-open">
    <div id="ui">
        <sling:call script="dialogs.jsp"/>
        <sling:include resourceType="composum/nodes/console/components/navbar"/>
        <div id="content-wrapper">
            <div id="split-view-horizontal-split" class="split-pane horizontal-split fixed-left">
                <div class="split-pane-component left-pane">
                    <div class="nodes-pckgmgr-tree-tabs">
                        <div class="nodes-pckgmgr-tree-tabs_head">
                            <div class="nodes-pckgmgr-tree-tabs_head-tab jcrpckg"><span
                                    class="nodes-pckgmgr-tree-tabs_head-label">${cpn:i18n(slingRequest,'Manager')}</span>
                            </div>
                            <div class="nodes-pckgmgr-tree-tabs_head-tab regpckg active"><span
                                    class="nodes-pckgmgr-tree-tabs_head-label">${cpn:i18n(slingRequest,'Registry')}</span>
                            </div>
                        </div>
                        <div class="nodes-pckgmgr-tree-tabs_body">
                            <sling:include resourceType="composum/nodes/pckgmgr/regpckg/tree"/>
                        </div>
                    </div>
                </div>
                <div class="split-pane-divider"></div>
                <div class="split-pane-component right-pane">
                    <div id="split-view-vertical-split" class="split-pane vertical-split fixed-top">
                        <div class="split-pane-component top-pane">
                            <div id="pckgmgr-query">
                                <sling:call script="query.jsp"/>
                            </div>
                        </div>
                        <div class="split-pane-divider"></div>
                        <div class="split-pane-component bottom-pane">
                            <div id="pckgmgr-view">
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
