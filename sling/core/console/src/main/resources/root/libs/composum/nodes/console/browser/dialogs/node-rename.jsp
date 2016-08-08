<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div id="node-rename-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog form-panel">
      <div class="modal-content">
        <form class="widget-form">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">Rename Node</h4>
          </div>
          <div class="modal-body">
            <div class="messages">
              <div class="alert"></div>
            </div>
            <input name="path" type="hidden">
            <input name="name" type="hidden">

            <div class="form-group">
              <label class="control-label">New Node Name</label>
              <input name="newname" class="widget text-field-widget form-control" type="text" placeholder="enter node name" data-rules="mandatory" autofocus>
            </div>
          </div>

          <div class="modal-footer buttons">
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary rename">Rename</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</cpn:component>
