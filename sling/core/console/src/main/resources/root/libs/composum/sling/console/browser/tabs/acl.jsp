<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
  <div class="acl detail-panel">
    <div class="acl-toolbar detail-toolbar">
      <div class="btn-group btn-group-sm" role="group">
        <button class="add fa fa-plus btn btn-default" title="Add policy to current node"><span class="label">Add</span></button>
        <button class="remove fa fa-minus btn btn-default" title="Remove selected policies"><span class="label">Remove</span></button>
      </div>
    </div>
    <div class="detail-content">
      <div class="split-pane vertical-split fixed-top">
        <div class="split-pane-component top-pane">
          <div class="table-container local-policies">
            <table class="table table-striped table-condensed" data-path="${browser.current.pathEncoded}">
            </table>
          </div>
        </div>
        <div class="split-pane-divider"><h4>Effective Policies</h4><span class="fa fa-ellipsis-h"></span></div>
        <div class="split-pane-component bottom-pane">
          <div class="table-container effective-policies">
            <table class="table table-striped table-condensed" data-path="${browser.current.pathEncoded}">
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</cpn:component>