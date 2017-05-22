<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div class="node-view-panel detail-panel something">
    <a class="favorite-toggle fa fa-star-o" href="#" title="Tooggle favorite state"><span class="label">Favorite</span></a>
    <div class="node-tabs detail-tabs action-bar btn-toolbar" role="toolbar">
      <div class="btn-group btn-group-sm" role="group">
        <a class="properties fa fa-list btn btn-default" href="#properties" data-group="properties" title="Node Properties"><span class="label">Properties</span></a>
        <c:if test="${browser.renderable}">
          <a class="view fa fa-eye btn btn-default" href="#display" data-group="view" title="Display Rendered View"><span class="label">View</span></a>
        </c:if>
        <a class="source json fa fa-code btn btn-default" href="#json" data-group="json" title="Source view as JSON (switchable to XML)"><span class="label">JSON</span></a>
        <a class="source xml fa fa-code btn btn-default hidden" href="#xml" data-group="xml" title="Source view as XML (switchable to JSON)"><span class="label">XML</span></a>
        <a class="acl fa fa-key btn btn-default" href="#acl" data-group="acl" title="Access Rules"><span class="label">ACL</span></a>
        <a class="version fa fa-history btn btn-default" href="#version" data-group="version" title="Versions"><span class="label">Versions</span></a>
      </div>
    </div>
    <div class="node-view-content detail-content">
    </div>
  </div>
</cpn:component>
