<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.VersionBean" scope="request">
    <%--@elvariable id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.VersionBean"--%>
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
                </a>
            </div>
            <sling:include replaceSelectors="status" />
        </div>
    </div>
</cpn:component>
