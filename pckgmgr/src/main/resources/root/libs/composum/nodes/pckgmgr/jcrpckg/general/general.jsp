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
                <a type="button" class="download fa fa-download btn btn-default" href="${pckg.downloadUrl}"
                   title="Download this package"><span class="label">Download</span></a>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="install btn btn-default btn-stack" title="(Re)Install this package"><span
                        class="fa-stack"><i class="fa fa-spin fa-gear fa-stack-2x background-text"></i><i
                        class="symbol fa fa-sign-in fa-stack-1x"></i><i
                        class="error fa fa-stack-2x">!</i></span><span
                        class="label">Install</span></button>
                <button class="assemble btn btn-default btn-stack" title="(Re)Build this package"><span
                        class="fa-stack"><i class="fa fa-spin fa-gear fa-stack-2x background-text"></i><i
                        class="symbol fa fa-archive fa-stack-1x"></i><i
                        class="error fa fa-stack-2x">!</i></span><span
                        class="label">Assemble</span></button>
                <button class="uninstall btn btn-default btn-stack" title="Uninstall this package"><span
                        class="fa-stack"><i class="fa fa-spin fa-gear fa-stack-2x background-text"></i><i
                        class="symbol fa fa-history fa-stack-1x"></i><i
                        class="error fa fa-stack-2x">!</i></span><span
                        class="label">Rewrap</span></button>
                <button class="delete fa fa-trash btn btn-default" title="Delete this package"><span class="label">Delete</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="create fa fa-plus btn btn-default" title="Create a new package"><span class="label">Create</span>
                </button>
                <button class="upload fa fa-upload btn btn-default" title="Upload a new package"><span class="label">Upload</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="refresh fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
        </div>
        <div class="package-detail">
            <div class="header-view panel panel-default">
                <sling:include replaceSelectors="header"/>
            </div>
            <div class="detail-view">
                <div class="aspect-view">
                    <div class="default-aspect panel panel-default">
                        <div class="panel-heading">Filter List</div>
                        <ul class="list-group">
                            <c:forEach items="${pckg.filterList}" var="filter">
                                <li class="list-group-item">${filter.root}</li>
                            </c:forEach>
                        </ul>
                    </div>
                    <div class="feedback-aspect panel panel-default hidden">
                        <div class="panel-heading"><span class="title"></span>&nbsp;
                            <button class="close" title="Close"><span class="fa fa-close"></span></button>
                        </div>
                        <div class="panel-body feedback-display">
                            <div class="log-output"></div>
                        </div>
                    </div>
                </div>
                <div class="audit-log panel panel-default">
                    <div class="panel-heading">
                        <div class="action-bar btn-toolbar toolbar">
                            <a class="audit-link" href="${pckg.auditLogUrl}">Audit Log</a>
                            <div class="btn-group btn-group-sm align-right" role="group">
                                <button type="button" class="refresh fa fa-refresh btn btn-default"
                                        title="Refresh Audit Log"><span
                                        class="label">Refresh</span></button>
                                <button type="button" class="purge fa fa-trash-o btn btn-default"
                                        title="Purge Audit Log"><span
                                        class="label">Purge</span></button>
                            </div>
                        </div>
                    </div>
                    <ul class="panel-body audit-list">
                    </ul>
                </div>
            </div>
        </div>
    </div>
</cpn:component>