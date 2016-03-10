<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <div class="detail-panel package ${pckg.cssClasses}">
        <div class="display-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <span class="resolver fa fa-external-link btn btn-default" title="Resolver Mapping ON/OFF"></span>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span></button>
                <button type="button" class="open fa fa-globe btn btn-default" title="Open in a separate view"><span class="label">Open</span></button>
            </div>
        </div>
        <div class="detail-content">
            <div class="header">
                <h3>${pckg.group}</h3>
                <h1>${pckg.name}</h1>
                <h2>${pckg.version}</h2>
                <div>created: ${pckg.created} by ${pckg.createdBy}</div>
                <div>last modified: ${pckg.lastModified} by ${pckg.lastModifiedBy}</div>
                <div><a href="${pckg.downloadUrl}">download: ${pckg.filename}</a></div>
            </div>
            <div class="filters">
                <ul>
                    <c:forEach items="${pckg.filterList}" var="filter">
                        <li>${filter.root}</li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>
</cpn:component>