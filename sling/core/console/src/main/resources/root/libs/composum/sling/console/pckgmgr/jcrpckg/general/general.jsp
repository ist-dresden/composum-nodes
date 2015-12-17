<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.PackageBean" scope="request">
    <div class="detail-panel package ${pckg.cssClasses}">
        <div class="header">
            <h3>${pckg.group}</h3>
            <h1>${pckg.name}</h1>
            <h2>${pckg.version}</h2>
            <div>created: ${pckg.created} by ${pckg.createdBy}</div>
            <div>last modified: ${pckg.lastModified} by ${pckg.lastModifiedBy}</div>
            <div><a href="${pckg.downloadUrl}">download: ${pckg.filename}</a></div>
        </div>
        <div class="coverage">
            <div class="table-container">
                <table class="coverage-table"
                       data-path="/bin/core/package.coverage.json${pckg.path}">
                </table>
            </div>
        </div>
    </div>
</cpn:component>