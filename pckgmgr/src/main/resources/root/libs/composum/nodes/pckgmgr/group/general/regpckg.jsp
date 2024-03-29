<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%
    long stopTime = System.currentTimeMillis() + 5000;
%>
<cpn:component id="bean" type="com.composum.sling.core.pckgmgr.regpckg.view.GroupBean" scope="request">
    <%--@elvariable id="bean" type="com.composum.sling.core.pckgmgr.regpckg.view.GroupBean"--%>
    <c:set var="writeAllowed" value="${bean.writeAllowed}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="detail-panel group ${pckg.cssClasses}">
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

        <div class="group-detail">
            <c:forEach items="${bean.packagePaths}" var="packagepath">
                <% if (System.currentTimeMillis() < stopTime) { %>
                <sling:include replaceSuffix="${packagepath}" replaceSelectors="listitem"
                               resourceType="composum/nodes/pckgmgr/regpckg/general"/>
                <% } %>
            </c:forEach>

            <% if (System.currentTimeMillis() >= stopTime) { %>
            <div class="alert alert-warning" role="alert">
                <cpn:text class="text" i18n="true">Too many packages to display.</cpn:text>
            </div>
            <% } %>
        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
