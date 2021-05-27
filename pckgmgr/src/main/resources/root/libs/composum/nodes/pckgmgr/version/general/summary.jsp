<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.VersionBean" scope="request">
    <sling:include replaceSelectors="status"/>
    <div class="status-block edit-state">
        <div><span class="fa fa-star-o"></span><span class="name">created: </span><span
                class="date">${pckg.created}</span> by ${pckg.createdBy}</div>
        <div><span class="fa fa-sign-in"></span><span class="name">installed: </span><span
                class="date">${pckg.installationTime}</span></div>
        <div><span class="fa fa-download"></span><a
                href="${pckg.downloadUrl}">${pckg.filename}</a></div>
    </div>
    <div class="status-block pack-state">
        <div><span class="fa fa-edit"></span><span class="name">last modified: </span><span
                class="date">${pckg.lastModified}</span> by ${pckg.lastModifiedBy}</div>
        <div><span class="fa fa-folder-o"></span><span class="name">last wrapped: </span><span
                class="date">${pckg.lastWrapped}</span> by ${pckg.lastWrappedBy}</div>
    </div>
</cpn:component>
