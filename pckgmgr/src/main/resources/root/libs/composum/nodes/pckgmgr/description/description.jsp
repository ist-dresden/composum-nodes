<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="description">
    <div class="row">
        <div class="left col col-xs-6">
            <sling:call script="short.jsp"/>
            <p>
                More documentation is <a href="https://www.composum.com/home/nodes/pckgmgr.html">available online.</a>
            </p>
        </div>
        <div class="left col col-xs-6">
            <ul>
                <li>uploading, downloading, and installing packages</li>
                <li>creation, editing and build of content packages</li>
                <li>inspection of package details like package options, filters, dependencies and content coverage
                </li>
                <li>reviewing package installation logs and reports</li>
                <li>rolling back package installations if necessary</li>
                <li>JCR package manager and package registry based interfaces</li>
                <li>package installation via the Apache Sling POST servlet</li>
            </ul>
        </div>
    </div>
    <ul class="nav nav-tabs">
        <li class="active"><a data-toggle="pill" href="#package-browser">Package Browser</a></li>
        <li><a data-toggle="pill" href="#filter-view">Filter View</a></li>
        <li><a data-toggle="pill" href="#coverage-view">Content Coverage</a></li>
        <li><a data-toggle="pill" href="#package-options">Package Options</a></li>
        <li><a data-toggle="pill" href="#registry-browser">Registry Browser</a></li>
    </ul>
    <div class="tab-content">
        <div id="package-browser" class="tab-pane fade active in">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/pckgmgr/description/images/package-browser.png'))}"
                 alt="Package Browser with Detail View"/>
        </div>
        <div id="filter-view" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/pckgmgr/description/images/filter-view.png'))}"
                 alt="Package Filter View"/>
        </div>
        <div id="coverage-view" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/pckgmgr/description/images/coverage-view.png'))}"
                 alt="Content Coverage View"/>
        </div>
        <div id="package-options" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/pckgmgr/description/images/package-options.png'))}"
                 alt="Package Options View"/>
        </div>
        <div id="registry-browser" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/pckgmgr/description/images/registry-browser.png'))}"
                 alt="Package Registry Browser"/>
        </div>
    </div>
</div>
