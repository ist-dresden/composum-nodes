<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<%-- <cpn:component id="usermanagement" type="com.composum.sling.core.usermanagement.UserManagement" scope="request"> --%>
<html>
<sling:call script="head.jsp"/>
<body id="usermanagement" class="console left-open top-open">
  <div id="ui">
    <sling:call script="dialogs.jsp"/>
    <sling:call script="/libs/composum/sling/console/page/navbar.jsp"/>
    <div id="content-wrapper">
      <div id="split-view-horizontal-split" class="split-pane horizontal-split fixed-left">
        <div class="split-pane-component left-pane">
          <div>
            <div class="tree-panel">
              <div id="browser-tree">
              </div>
            </div>
          </div>
        </div>
        <div class="split-pane-divider"><span class="fa fa-ellipsis-v"></span></div>
        <div class="split-pane-component right-pane">
          <div id="browser-view">
            <sling:call script="view.jsp"/>
          </div>
        </div>
      </div>
    </div>
  </div>
<sling:call script="script.jsp"/>
</body>
</html>
<%-- </cpn:component> --%>
