<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
  <div class="script detail-panel">
    <div class="widget code-editor-widget">
      <div class="editor-toolbar detail-toolbar">
        <div class="btn-group btn-group-sm" role="group">
          <button type="button" class="run-script fa fa-play btn btn-default" title="Run Script"><span class="label">Run Script</span></button>
        </div>
        <div class="btn-group btn-group-sm" role="group">
          <div class="search input-group input-group-sm">
            <input type="text" class="find-text form-control" placeholder="search in text">
            <span class="find-prev fa fa-chevron-left input-group-addon" title="find previous"></span>
            <span class="find-next fa fa-chevron-right input-group-addon" title="find next"></span>
          </div>
        </div>
        <div class="btn-group btn-group-sm" role="group">
          <button type="button" class="start-editing fa fa-pencil btn btn-default" title="Edit Script"><span class="label">Edit</span></button>
        </div>
        <div class="btn-group btn-group-sm" role="group">
          <a href="" class="download fa fa-download btn btn-default" title="Download text file"><span class="label">Download</span></a>
          <button type="button" class="upload fa fa-upload btn btn-default" title="Upload text file"><span class="label">Upload</span></button>
        </div>
      </div>
      <div class="detail-content">
        <div id="script-view" class="split-pane vertical-split fixed-bottom">
          <div class="split-pane-component top-pane">
            <div class="editor-frame">
              <div class="code-editor" data-path="${browser.contentResource.path}" data-type="${browser.textType}">
              </div>
            </div>
          </div>
          <div class="split-pane-divider"><span class="fa fa-ellipsis-h"></span></div>
          <div class="split-pane-component bottom-pane">
            <div class="log-output">log output...</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</cpn:component>