<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpn:defineObjects/>
<cpn:component id="console" type="com.composum.sling.nodes.console.ConsoleModel">
    <header class="navbar navbar-inverse navbar-fixed-top bs-docs-nav" role="banner">
        <div class="navbar-header">
            <button class="navbar-toggle" type="button" data-toggle="collapse" data-target=".bs-navbar-collapse">
                <span class="sr-only">Toggle navigation</span> <span class="icon-bar"></span> <span
                    class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <div id="composum-logo" class="navbar-brand">
                <a href="${cpn:url(slingRequest,cpn:cpm('composum/nodes/console/content'))}">
                    <cpn:image src="${composumBase}composum/nodes/console/page/images/composum-nodes-logo-on-black.png" alt=""/>
                </a>
            </div>
        </div>
        <nav class="collapse navbar-collapse bs-navbar-collapse" role="navigation">
            <sling:call script="consoles.jsp"/>
            <ul class="system nav navbar-nav navbar-right">
                <c:if test="${console.supportsPermissions}">
                    <li>
                        <button type="button" class="nav-permissions-status btn btn-default"
                                data-user="${console.userPermission}" data-system="${console.systemPermission}"
                                title=""><i class="fa"></i></button>
                    </li>
                </c:if>
                <li>
                    <a class="nav-user-status"><em>${console.currentUser}</em></a>
                    <a class="system-health-monitor delimiter" tabindex="0" role="button"
                       title="${cpn:i18n(slingRequest,'System Health')}" data-trigger="focus"
                       data-container="body" data-toggle="popover" data-placement="bottom"
                       data-content="Vivamus sagittis lacus vel augue laoreet rutrum faucibus."><span></span>
                        <div class="system-health-popover hidden">
                            <sling:include resourceType="composum/nodes/commons/components/system"/>
                        </div>
                    </a>
                    <a class="nav-workspace-menu" data-toggle-x="dropdown"
                       title="System configuration..."><em>${console.workspaceName}</em></a>
                        <%-- ul class="dropdown-menu" role="menu">
                          <li><a href="#" class="workspaces" title="Open workspaces tool">Workspaces...</a></li>
                        </ul --%>
                </li>
            </ul>
        </nav>
    </header>
</cpn:component>
