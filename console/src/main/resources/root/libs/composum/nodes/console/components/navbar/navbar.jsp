<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<cpn:component id="status" type="com.composum.sling.nodes.console.Consoles">
  <header class="navbar navbar-inverse navbar-fixed-top bs-docs-nav" role="banner">
    <div class="navbar-header">
      <button class="navbar-toggle" type="button" data-toggle="collapse" data-target=".bs-navbar-collapse">
        <span class="sr-only">Toggle navigation</span> <span class="icon-bar"></span> <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <div id="composum-logo" class="navbar-brand">
        <a href="${cpn:url(slingRequest,'/libs/composum/nodes/console/content')}">
          <cpn:image src="/libs/composum/nodes/console/page/images/composum-nodes-logo-on-black.png" alt=""/>
        </a>
      </div>
    </div>
    <nav class="collapse navbar-collapse bs-navbar-collapse" role="navigation">
      <sling:call script="consoles.jsp"/>
      <ul class="system nav navbar-nav navbar-right">
        <li>
          <a class="nav-user-status"><em>${status.currentUser}</em></a>
          <span class="delimiter">@</span>
          <a class="nav-workspace-menu" data-toggle="dropdown" title="System configuration..."><em>${status.workspaceName}</em></a>
          <ul class="dropdown-menu" role="menu">
            <li><a href="#" class="workspaces" title="Open workspaces tool">Workspaces...</a></li>
          </ul>
        </li>
      </ul>
    </nav>
  </header>
</cpn:component>
