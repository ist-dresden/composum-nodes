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
                <button class="build fa fa-folder-open btn btn-default" title="(Re)Build the package"><span
                        class="label">Unwrap</span>
                </button>
                <button class="rewrap fa fa-folder btn btn-default" title="Rewrap the package"><span
                        class="label">Rewrap</span></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="upload fa fa-upload btn btn-default" title="Upload a new package"><span class="label">Upload</span>
                </button>
                <a type="button" class="download fa fa-download btn btn-default" href="${pckg.downloadUrl}"
                   title="Download selected package"><span class="label">Download</span></a>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="create fa fa-plus btn btn-default" title="Create a new package"><span class="label">Create</span>
                </button>
                <button class="delete fa fa-minus btn btn-default" title="Delete this package"><span class="label">Delete</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="refresh fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
        </div>
        <div class="package-detail">
            <div class="panel panel-default default-aspect">
                <sling:include replaceSelectors="header"/>
                <div class="panel-heading">Filter List</div>
                <ul class="list-group">
                    <c:forEach items="${pckg.filterList}" var="filter">
                        <li class="list-group-item">${filter.root}</li>
                    </c:forEach>
                </ul>
            </div>
            <div class="panel panel-default feedback-aspect hidden">
                <sling:include replaceSelectors="header"/>
                <div class="panel-heading"><span class="title"></span>&nbsp;
                    <button class="close" title="Close"><span class="fa fa-close"></span></button>
                </div>
                <div class="panel-body feedback-display">
                    <table></table>
                </div>
            </div>
        </div>
    </div>
</cpn:component>