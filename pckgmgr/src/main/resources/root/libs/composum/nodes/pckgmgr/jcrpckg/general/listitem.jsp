<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageBean" scope="request">
    <div class="pckg-list-item ${pckg.cssClasses} panel panel-default">
        <div class="panel-heading header">
            <div class="thumbnail">
                <div class="image-wrapper">
                    <img src="${pckg.thumbnailUrl}"/>
                </div>
            </div>
            <div class="identifiers">
                <a href="${pckg.url}" data-path="${pckg.path}">
                    <h3>${cpn:text(pckg.group)}&nbsp;</h3>
                    <h1>${cpn:text(pckg.name)}</h1>
                    <h2>${cpn:text(pckg.version)}&nbsp;</h2>
                    <div>last modified: ${pckg.lastModified} by ${cpn:text(pckg.lastModifiedBy)}</div>
                    <c:if test="${pckg.installed}">
                        <div><span class="name">last unpacked: </span><span
                                        class="date">${pckg.lastUnpacked}</span> by ${cpn:text(pckg.lastUnpackedBy)}</div>
                    </c:if>
                </a>
                <c:if test="${pckg.hasAlternativeVersions && cpn:contains(slingRequest.requestPathInfo.selectors, 'listalternativeversions')}">
                    <div class="active-versions">
                        alternative versions present:
                        <c:forEach items="${pckg.allVersions}" var="version">
                            <%--@elvariable id="version" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageBean"--%>
                            <span class="indicator-block ${version.cssClasses}" title="${version.installed ? 'installed' : 'not installed'}">
                                <a href="${version.url}" class="status-indicator install-status">
                                        ${cpn:text(version.version)}
                                </a>
                            </span>
                        </c:forEach>
                    </div>
                </c:if>
            </div>
            <sling:include replaceSelectors="status" />
        </div>
    </div>
</cpn:component>
