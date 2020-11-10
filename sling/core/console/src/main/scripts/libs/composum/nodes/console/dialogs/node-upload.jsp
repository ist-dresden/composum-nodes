<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div id="node-upload-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content form-panel default">

        <cpn:form classes="widget-form" enctype="multipart/form-data"
                  action="/bin/cpm/nodes/node.map.json">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">Uplode Node</h4>
          </div>
          <div class="modal-body">
            <div class="messages">
              <div class="alert"></div>
            </div>

            <input name="_charset_" type="hidden" value="UTF-8" />
            <div class="form-group">
              <label class="control-label">Parent Path</label>
              <div class="path input-group widget path-widget" data-rules="mandatory">
                <input name="path" class="form-control" type="text" />
                <span class="input-group-btn">
                  <button class="select btn btn-default" type="button" title="Select Repository Path">...</button>
                </span>
              </div>
            </div>
            <div class="form-group">
              <label class="control-label">New Node Name</label>
              <input name="name" class="widget text-field-widget form-control" type="text"
                     placeholder="enter node name" data-rules="mandatory" />
            </div>
            <div class="form-group">
              <label class="control-label">Node File (JSON)</label>
              <input name="file" class="widget file-upload-widget form-control" type="file"
                     data-options="hidePreview"/>
            </div>
            <!--div class="form-group">
              <label class="control-label">Mapping Policy</label>
              <select name="rule" class="widget checkbox-widget form-control">
              </select>
            </div-->
          </div>

          <div class="modal-footer buttons">
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-primary upload">Upload</button>
          </div>
        </cpn:form>
      </div>
    </div>
  </div>
</cpn:component>