<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean" scope="request">
    <%--@elvariable id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean"--%>
    <div class="pckg-list-item ${pckg.cssClasses} panel panel-default">
        <div class="identifiers">
            <a href="${pckg.url}" data-path="${pckg.path}">
                <h3>${cpn:text(pckg.group)}&nbsp;</h3>
                <h1>${cpn:text(pckg.name)}</h1>
                <h2>${cpn:text(pckg.version)}&nbsp;</h2>
            </a>
        </div>
        <div class="obsolete-versions checkbox-group" role="group">
            <c:forEach items="${pckg.obsoleteVersions}" var="version">
                <%--@elvariable id="version" type="com.composum.sling.core.pckgmgr.regpckg.view.VersionBean"--%>
                <label class="checkbox-control">
                    <input name="packageId" value="${version.packageId}" class="checkbox-field" type="checkbox" checked="checked"/>
                    <span class="checkbox-label">${cpn:text(version.version)}</span>
                </label>
            </c:forEach>
        </div>
    </div>
</cpn:component>
