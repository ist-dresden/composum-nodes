<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <div class="detail-panel package ${pckg.cssClasses}">
        <div class="display-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="edit fa fa-pencil btn btn-default" title="Edit package properties"><span class="label">Edit</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="install fa fa-sign-in btn btn-default" title="(Re)Install the package"><span class="label">(Re)Install</span></button>
                <button class="build fa fa-suitcase btn btn-default" title="(Re)Build the package"><span class="label">(Re)Build</span></button>
                <button class="rewrap fa fa-magic btn btn-default" title="Rewrap the package"><span class="label">Rewrap</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="upload fa fa-upload btn btn-default" title="Upload a new package"><span class="label">Upload</span></button>
                <button class="download fa fa-download btn btn-default" title="Download selected package"><span class="label">Download</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="create fa fa-plus btn btn-default" title="Create a new package"><span class="label">Create</span></button>
                <button class="delete fa fa-minus btn btn-default" title="Delete this package"><span class="label">Delete</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span></button>
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