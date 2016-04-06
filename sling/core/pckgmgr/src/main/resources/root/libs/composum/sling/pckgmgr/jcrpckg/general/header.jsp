<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
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
            <div><span class="fa fa-star-o"></span><span class="name">created: </span><span
                    class="date">${pckg.created}</span> by ${pckg.createdBy}</div>
            <div><span class="fa fa-edit"></span><span class="name">last modified: </span><span
                    class="date">${pckg.lastModified}</span> by ${pckg.lastModifiedBy}</div>
            <div><span class="fa fa-download"></span> <a
                    href="${pckg.downloadUrl}">download: ${pckg.filename}</a></div>
        </div>
        <div class="status-block pack-state">
            <div><span class="fa fa-sign-in"></span><span class="name">last unpacked: </span><span
                    class="date">${pckg.lastUnpacked}</span> by ${pckg.lastUnpackedBy}</div>
            <div><span class="fa fa-folder-open-o"></span><span class="name">last unwrapped: </span><span
                    class="date">${pckg.lastUnwrapped}</span> by ${pckg.lastUnwrappedBy}</div>
            <div><span class="fa fa-folder-o"></span><span class="name">last wrapped: </span><span
                    class="date">${pckg.lastWrapped}</span> by ${pckg.lastWrappedBy}</div>
        </div>
    </div>
</cpn:component>