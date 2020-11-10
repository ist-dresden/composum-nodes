<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div class="properties detail-panel full-table-view">
    <div class="table-toolbar">
      <div class="btn-group btn-group-sm" role="group">
        <button class="add fa fa-plus btn btn-default" title="Add new property"><span class="label">Add</span></button>
        <button class="remove fa fa-minus btn btn-default" title="Remove selected properties"><span class="label">Remove</span></button>
      </div>
      <div class="btn-group btn-group-sm" role="group">
        <button class="copy fa fa-copy btn btn-default" title="Copy selected properties"><span class="label">Copy</span></button>
        <button class="paste fa fa-paste btn btn-default" title="Paste copied properties"><span class="label">Paste</span></button>
      </div>
      <div class="btn-group btn-group-sm" role="group">
        <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
        </button>
      </div>
    </div>
    <div class="table-container">
      <table id="browser-view-property-table" class="property-table"
             data-path="${browser.current.pathEncoded}"
             data-toolbar=".node-view-panel .properties .table-toolbar">
      </table>
    </div>
  </div>
</cpn:component>