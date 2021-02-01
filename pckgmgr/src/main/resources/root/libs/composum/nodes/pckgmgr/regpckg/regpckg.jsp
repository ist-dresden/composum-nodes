<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.view.PackageManagerBean" scope="request">
    <div class="detail-panel jcrpckg">
        <div class="detail-tabs action-bar btn-toolbar" role="toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <a class="package fa fa-suitcase btn btn-default" href="#general" data-group="general" title="Package"><span class="label">Package</span></a>
                <a class="filter fa fa-filter btn btn-default" href="#filter" data-group="filter" title="Filter"><span class="label">Filter</span></a>
                <a class="coverage fa fa-list btn btn-default" href="#coverage" data-group="content" title="Coverage"><span class="label">Coverage</span></a>
                <a class="options fa fa-tags btn btn-default" href="#options" data-group="options" title="Options"><span class="label">Options</span></a>
            </div>
        </div>
        <div class="detail-content">
        </div>
    </div>
</cpn:component>
