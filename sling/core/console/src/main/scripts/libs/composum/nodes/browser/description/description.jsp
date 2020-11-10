<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="description">
    <div class="row">
        <div class="left col col-xs-6">
            <sling:call script="short.jsp"/>
        </div>
        <div class="left col col-xs-6">
            <ul>
                <li>repository tree with configurable filters</li>
                <li>node creation / deletion / move / reorder</li>
                <li>property manipulation</li>
                <li>binary data upload / download</li>
                <li>component rendering and asset view</li>
                <li>template based queries with history</li>
                <li>JSON view, download and upload</li>
                <li>ACL view and manipulation</li>
            </ul>
        </div>
    </div>
    <ul class="nav nav-tabs">
        <li class="active"><a data-toggle="pill" href="#browser-props">Repository Browser</a></li>
        <li><a data-toggle="pill" href="#browser-view">Render / Image View</a></li>
        <li><a data-toggle="pill" href="#browser-code">Code View and Editing</a></li>
    </ul>
    <div class="tab-content">
        <div id="browser-props" class="tab-pane fade active in">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,'/libs/composum/nodes/browser/description/images/browser-props.png')}"
                 alt="Repository Browser and Query View"/>
        </div>
        <div id="browser-view" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,'/libs/composum/nodes/browser/description/images/browser-view.png')}"
                 alt="Repository Browser Render View"/>
        </div>
        <div id="browser-code" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,'/libs/composum/nodes/browser/description/images/browser-code.png')}"
                 alt="Repository Browser Code View"/>
        </div>
    </div>
</div>
