<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
  <div id="browser-view-property-dialog" class="change property dialog modal fade"
       tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content form-panel default">

        <cpn:form classes="widget-form default" enctype="multipart/form-data"
                  action="/bin/core/property.bin">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">Create or Change Property</h4>
          </div>

          <div class="modal-body">
            <div class="messages">
              <div class="alert"></div>
            </div>
            <input name="path" type="hidden">
            <input name="oldname" type="hidden">
            <div class="form-group type">
              <label class="control-label">Type</label>
              <select name="type" class="widget property-type-widget form-control">
              </select>
            </div>
            <div class="form-group multi">
              <label class="control-label">Multi</label>
              <input name="multi" class="multi-select-box form-control widget checkbox-widget" type="checkbox">
            </div>
            <div class="form-group name">
              <label class="control-label">Name</label>
              <input name="name" class="widget property-name-widget form-control" type="text"
                     placeholder="property name" data-rules="mandatory">
            </div>
            <sling:call script="/libs/composum/sling/console/browser/dialogs/property-value.jsp" />
          </div>

          <div class="modal-footer buttons">
            <button type="button" class="btn btn-danger delete">Delete</button>
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-primary default save">Save</button>
            <button type="button" class="btn btn-primary binary upload">Upload</button>
          </div>

        </cpn:form>
      </div>
    </div>
  </div>
</cpn:component>
