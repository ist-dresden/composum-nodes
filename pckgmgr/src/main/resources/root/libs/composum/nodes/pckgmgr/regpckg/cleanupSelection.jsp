<%@page session="false" pageEncoding="utf-8" %>
<%-- Selection of obsolete versions for package-cleanup dialog. --%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean" scope="request">
    <%--@elvariable id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean"--%>
    <div class="pckg-list-item detail-panel group ${pckg.cssClasses} panel panel-default">
        <div class="panel-heading header detail-panel">
            <div class="thumbnail">
                <div class="image-wrapper">
                    <img src="${pckg.currentVersion.thumbnailUrl}"/>
                </div>
            </div>
            <div class="identifiers form-group">
                <a href="${pckg.url}" data-path="${pckg.path}">
                    <h3>${cpn:text(pckg.group)}&nbsp;</h3>
                    <h1>${cpn:text(pckg.name)}</h1>
                    <h2>${cpn:text(pckg.version)}&nbsp;</h2>
                </a>
                <div class="status-block">
                    <div><span class="fa fa-star-o"></span><span class="name">created: </span><span
                            class="date">${pckg.currentVersion.created}</span></div>
                    <div><span class="fa fa-sign-in"></span><span class="name">installed: </span><span
                            class="date">${pckg.currentVersion.installationTime}</span></div>
                </div>
            </div>
            <sling:include replaceSelectors="status" resourceType="composum/nodes/pckgmgr/version/general" replaceSuffix="${pckg.currentVersion.path}"/>
        </div>
        <div class="panel-body">
            <div class="obsolete-versions form-group right checkbox-group" role="group">
                <c:forEach items="${pckg.obsoleteVersions}" var="version">
                    <%--@elvariable id="version" type="com.composum.sling.core.pckgmgr.regpckg.view.VersionBean"--%>
                    <label class="checkbox-control">
                        <input name="cleanupVersion" value="${version.path}" class="checkbox-field cleanup-version" type="checkbox"
                               checked="checked"/>
                        <span class="checkbox-label">${cpn:text(version.version)}</span>
                        <div class="status-block edit-state">
                            <span class="fa fa-star-o"></span><span class="name">created: </span><span
                                    class="date">${version.created}</span>
                            <span class="fa fa-sign-in"></span><span class="name">installed: </span><span
                                    class="date">${version.installationTime}
                        </div>
                    </label>
                </c:forEach>
            </div>
        </div>
    </div>
</cpn:component>
