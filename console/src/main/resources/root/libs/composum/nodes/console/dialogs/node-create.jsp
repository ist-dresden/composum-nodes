<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div id="node-create-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content form-panel default">

        <cpn:form classes="widget-form" enctype="multipart/form-data"
                  action="/bin/cpm/nodes/node.create.json">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">Create New Node</h4>
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
              <label class="control-label">Primary Type</label>
              <input name="type" class="widget primary-type-widget form-control" type="text"
                     placeholder="enter primary type" autofocus data-rules="mandatory" />
            </div>
            <div class="form-group">
              <label class="control-label">New Node Name</label>
              <input name="name" class="widget text-field-widget form-control" type="text"
                     placeholder="enter node name" data-rules="mandatory" />
            </div>
            <div class="form-group default sling">
              <label class="control-label">Title <span>('jcr:title'; optional)</span></label>
              <input name="title" class="widget text-field-widget form-control" type="text"
                     placeholder="enter the title of the new node" />
            </div>
            <div class="form-group sling">
              <label class="control-label">Sling Resource Type <span>(optional)</span></label>
              <input name="resourceType" class="widget text-field-widget form-control" type="text"
                     placeholder="enter the Sling resource type if useful" />
            </div>
            <div class="form-group index">
              <label class="control-label">Index Type (property | ordered)</label>
              <input name="indexType" class="widget text-field-widget form-control" type="text"
                     placeholder="enter the type of index" />
            </div>
            <div class="form-group linked">
              <label class="control-label">Linked File Path</label>
              <div class="path input-group widget path-widget" data-filter="referenceable">
                <input name="jcrContent" class="form-control" type="text" />
                <span class="input-group-btn">
                  <button class="select btn btn-default" type="button" title="Select Repository Path">...</button>
                </span>
              </div>
            </div>
            <div class="form-group binary">
              <label class="control-label">Mime Type</label>
              <input name="mimeType" type="text" class="form-control" />
            </div>
            <div class="form-group binary">
              <label class="control-label">Upload File</label>
              <input name="file" class="widget file-upload-widget form-control" type="file"
                     data-options="hidePreview"/>
            </div>
          </div>

          <div class="modal-footer buttons">
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary create">Create</button>
          </div>
        </cpn:form>
      </div>
    </div>
  </div>
</cpn:component>