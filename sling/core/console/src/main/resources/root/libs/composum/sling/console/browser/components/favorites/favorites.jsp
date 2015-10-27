<%@page session="false" pageEncoding="utf-8"%><%--
 *
 * Favorites Overlay - HTML
 *
--%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%><%--
--%><%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%><%--
--%><%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%--
--%><sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
  <div id="favorites-overlay" class="favorites-overlay hidden">
    <div class="favorites-component">
      <div class="marked-nodes panel panel-default">
        <div class="panel-heading">marked favorites</div>
        <ul class="list-group scrollable">
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
        </ul>
      </div>
      <div class="recently-used panel panel-default">
        <div class="panel-heading">recently used</div>
        <ul class="list-group scrollable">
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
          <a href="#" class="list-group-item template" data-path="">
            <i class="fa"></i><h4 class="name">Node name</h4>
            <h5 class="path">/content/test/some/node</h5>
          </a>
        </ul>
      </div>
    </div>
    <div class="tree-actions action-bar btn-toolbar" role="toolbar">
      <div class="align-left">
      </div>
      <div class="align-right">
        <div class="btn-group btn-group-sm" role="group">
          <button type="button" class="toggle glyphicon-star glyphicon btn btn-default" title="Close favorites list"><span class="label">Close / switch to tree</span></button>
        </div>
      </div>
    </div>
  </div>
</cpn:component>
