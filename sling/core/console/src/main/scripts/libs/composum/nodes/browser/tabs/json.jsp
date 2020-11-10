<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="json detail-panel">
        <div class="json-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <div class="depth input-group input-group-sm widget number-field-widget">
                    <span class="fa fa-level-down input-group-addon text-muted" title="content depth"></span>
                    <input type="text" class="form-control"/>
                    <span class="input-group-addon spinner">
              <span class="decrement fa fa-minus" title="decrement"></span>
              <span class="increment fa fa-plus" title="increment"></span>
          </span>
                </div>
            </div>
            <div class="props btn-group btn-group-sm widget select-buttons-widget">
                <button type="button" data-value="source" class="fa fa-sticky-note-o btn btn-default"
                        title="render as JSON source"><span class="label">Source</span></button>
                <button type="button" data-value="notype" class="fa fa-times btn btn-default"
                        title="no type hints in values"><span class="label">no type</span></button>
                <button type="button" data-value="type" class="fa fa-exclamation btn btn-default"
                        title="type hints in values"><span class="label">no type</span></button>
            </div>
            <div class="binary btn-group btn-group-sm widget select-buttons-widget">
                <button type="button" data-value="link" class="fa fa-external-link btn btn-default"
                        title="embed link to binary data"><span class="label">Link</span></button>
                <button type="button" data-value="base64" class="fa fa-plus-square btn btn-default"
                        title="embed binary data base64 encoded"><span class="label">Base 64</span></button>
                <button type="button" data-value="skip" class="fa fa-minus-square-o btn btn-default"
                        title="skip binary data"><span class="label">Skip</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <div class="indent input-group input-group-sm widget number-field-widget" data-options="0:2:4">
                    <span class="fa fa-indent input-group-addon text-muted" title="text indent"></span>
                    <input type="text" class="form-control"/>
                    <span class="input-group-addon spinner">
              <span class="decrement fa fa-minus" title="decrement"></span>
              <span class="increment fa fa-plus" title="increment"></span>
          </span>
                </div>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="copy fa fa-copy btn btn-default" title="Copy JSON to clipboard"><span
                        class="label">Copy</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <a href="" class="download fa fa-download btn btn-default" title="Download as JSON file"><span
                        class="label">Download</span></a>
                <button type="button" class="upload fa fa-upload btn btn-default"
                        title="Update from an uploaded JSON file"><span class="label">Upload</span></button>
            </div>
            <div class="menu btn-group btn-group-sm dropdown" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload JSON view"><span
                        class="label">Reload</span></button>
                <button type="button" class="fa fa-bars btn btn-default dropdown-toggle"
                        data-toggle="dropdown" title="Source type..."><span class="label">Source type...</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-right" role="menu">
                    <li class="active"><a href="#json" class="json"
                                          title="Show a JSON view of the source">JSON</a></li>
                    <li><a href="#xml" class="xml"
                           title="Show a XML view of the source">XML</a></li>
                </ul>
            </div>
        </div>
        <div class="embedded frame-container detail-content">
            <iframe src="" width="100%" height="100%"></iframe>
        </div>
    </div>
</cpn:component>