<%@page session="false" pageEncoding="utf-8"%><%--
 *
 * Favorites Overlay - HTML
 *
--%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%><%--
--%><%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%><%--
--%><%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%--
--%><sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
  <div id="favorites-overlay" class="hidden">
    <div class="favorites-panel split-pane vertical-split fixed-bottom">
      <div class="split-pane-component top-pane">
        <div class="marked-nodes">
          <ol class="list scrollable">
          </ol>
        </div>
      </div>
      <div class="split-pane-divider"><h3>used recently</h3><span class="fa fa-ellipsis-h"></span></div>
      <div class="split-pane-component bottom-pane">
        <div class="used-recently">
          <ol class="list scrollable">
          </ol>
        </div>
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
    <div class="template">
      <a href="#" data-path="">
        <i class="fa"></i><h4 class="name">Node name</h4>
        <h5 class="path">/content/test/some/node</h5>
      </a>
    </div>
  </div>
</cpn:component>
