<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>

<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean" scope="request">
    <%--@elvariable id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.PackageBean"--%>
    <c:set var="writeAllowed" value="${pckg.writeAllowed}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="detail-panel package">
        <div class="display-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="refresh fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="cleanup fa fa-recycle btn btn-default"${writeDisabled}
                        title="Cleanup obsolete package versions"><span class="label">Cleanup</span></button>
            </div>
            <sling:include resourceType="composum/nodes/pckgmgr" replaceSelectors="helpbutton"/>
        </div>

        <div class="package-detail">
            <c:forEach items="${pckg.allVersions}" var="version">
                <sling:include replaceSuffix="${version.path}" replaceSelectors="listitem"
                               resourceType="composum/nodes/pckgmgr/version/general"/>
            </c:forEach>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
