<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="xml detail-panel" data-path="${browser.current.pathEncoded}">
        <div class="xml-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="copy fa fa-copy btn btn-default" title="Copy XML to clipboard"><span
                        class="label">Copy</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <a href="" class="download fa fa-download btn btn-default" title="Download as XML file" target="_blank"><span
                        class="label">Download</span></a>
                <a href="" class="zip fa fa-file-archive-o btn btn-default" title="Download whole tree as ZIP file"
                   target="_blank"><span class="label">Zip</span></a>
                <a href="" class="pkg fa fa-suitcase btn btn-default" title="Download whole tree as package ZIP file"
                   target="_blank"><span class="label">Package</span></a>
            </div>
            <div class="menu btn-group btn-group-sm dropdown" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
                <button type="button" class="fa fa-bars btn btn-default dropdown-toggle"
                        data-toggle="dropdown" title="Source type..."><span class="label">Source type...</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-right" role="menu">
                    <li><a href="#json" class="json"
                           title="Show a JSON view of the source">JSON</a></li>
                    <li class="active"><a href="#xml" class="xml"
                                          title="Show a XML view of the source">XML</a></li>
                </ul>
            </div>
        </div>
        <div class="embedded frame-container detail-content">
            <iframe src="" width="100%" height="100%"></iframe>
        </div>
    </div>
</cpn:component>
