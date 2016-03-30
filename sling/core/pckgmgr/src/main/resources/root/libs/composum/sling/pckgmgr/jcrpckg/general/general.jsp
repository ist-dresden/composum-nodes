<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <div class="detail-panel package ${pckg.cssClasses}">
        <div class="display-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="edit fa fa-pencil btn btn-default" title="Edit package properties"><span class="label">Edit</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="install fa fa-sign-in btn btn-default" title="(Re)Install the package"><span
                        class="label">(Re)Install</span></button>
                <button class="build fa fa-folder-open btn btn-default" title="(Re)Build the package"><span class="label">Unwrap</span>
                </button>
                <button class="rewrap fa fa-folder btn btn-default" title="Rewrap the package"><span
                        class="label">Rewrap</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="upload fa fa-upload btn btn-default" title="Upload a new package"><span class="label">Upload</span>
                </button>
                <button class="download fa fa-download btn btn-default" title="Download selected package"><span
                        class="label">Download</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="create fa fa-plus btn btn-default" title="Create a new package"><span class="label">Create</span>
                </button>
                <button class="delete fa fa-minus btn btn-default" title="Delete this package"><span class="label">Delete</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
        </div>
        <div class="package-detail">
            <div class="panel panel-default">
                <div class="panel-heading header">
                    <div class="thumbnail">
                        <a class="upload-thumbnail" href="#" title="Upload a new thumbnail image (64x64)">
                            <div class="image-wrapper">
                                <img src="${pckg.thumbnailUrl}"/>
                            </div>
                        </a>
                    </div>
                    <div class="identifiers">
                        <h3>${pckg.group}&nbsp;</h3>
                        <h1>${pckg.name}</h1>
                        <h2>${pckg.version}&nbsp;</h2>
                    </div>
                    <div class="description">${pckg.description}</div>
                </div>
                <div class="panel-body status">
                    <sling:include replaceSelectors="status"/>
                    <div class="status-block edit-state">
                        <div><span class="fa fa-star-o"></span><span class="name">created: </span><span class="date">${pckg.created}</span> by ${pckg.createdBy}</div>
                        <div><span class="fa fa-edit"></span><span class="name">last modified: </span><span class="date">${pckg.lastModified}</span> by ${pckg.lastModifiedBy}</div>
                        <div><span class="fa fa-download"></span> <a href="${pckg.downloadUrl}">download: ${pckg.filename}</a></div>
                    </div>
                    <div class="status-block pack-state">
                        <div><span class="fa fa-sign-in"></span><span class="name">last unpacked: </span><span class="date">${pckg.lastUnpacked}</span> by ${pckg.lastUnpackedBy}</div>
                        <div><span class="fa fa-folder-open-o"></span><span class="name">last unwrapped: </span><span class="date">${pckg.lastUnwrapped}</span> by ${pckg.lastUnwrappedBy}</div>
                        <div><span class="fa fa-folder-o"></span><span class="name">last wrapped: </span><span class="date">${pckg.lastWrapped}</span> by ${pckg.lastWrappedBy}</div>
                    </div>
                </div>
                <div class="panel-heading">Filter List</div>
                <ul class="list-group">
                    <c:forEach items="${pckg.filterList}" var="filter">
                        <li class="list-group-item">${filter.root}</li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>
</cpn:component>