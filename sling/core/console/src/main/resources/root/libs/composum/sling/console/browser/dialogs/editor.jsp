<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
  <div id="text-edit-dialog" class="dialog modal fade" role="dialog" aria-labelledby="Edit Text"
       aria-hidden="true">
    <div class="text-editor detail-panel">
      <div class="modal-dialog">
        <div class="modal-content">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="modal-title"></h4>
          </div>

          <div class="modal-body widget code-editor-widget">
            <div class="code-editor" data-path="${browser.contentResource.path}" data-type="${browser.textType}">
            </div>
          </div>

          <div class="modal-footer buttons">
            <div class="toolbar">
            </div>
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-primary save">Save</button>
          </div>

        </div>
      </div>
    </div>
  </div>
</cpn:component>