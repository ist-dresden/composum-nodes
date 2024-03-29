<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%
    long stopTime = System.currentTimeMillis() + 5000;
%>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
    <%--@elvariable id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean"--%>
    <c:set var="writeAllowed" value="${pckgmgr.writeAllowed}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>
    <div class="detail-panel group">
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

        <div class="group-detail panel panel-default">

            <c:if test="${not empty pckgmgr.pathsToVersionsOfThisPackage}">
                <div class="panel-heading">
                    <cpn:text class="text" i18n="true">Package Versions:</cpn:text>
                </div>

                <c:forEach items="${pckgmgr.pathsToVersionsOfThisPackage}" var="pckgpath">
                    <sling:include replaceSuffix="${pckgpath}" replaceSelectors="listitem"
                                   resourceType="composum/nodes/pckgmgr/jcrpckg/general"/>
                </c:forEach>
            </c:if>

            <div class="subgroups">
                <c:if test="${not empty pckgmgr.pathsToHighestVersionOfEachPackage}">
                    <div class="panel-heading">
                        <cpn:text class="text" i18n="true">Packages of this group and subgroups:</cpn:text>
                    </div>

                    <c:forEach items="${pckgmgr.pathsToHighestVersionOfEachPackage}" var="pckgpath">
                        <% if (System.currentTimeMillis() < stopTime) { %>
                        <sling:include replaceSuffix="${pckgpath}" replaceSelectors="listitem.listalternativeversions"
                                       resourceType="composum/nodes/pckgmgr/jcrpckg/general"/>
                        <% } %>
                    </c:forEach>

                    <% if (System.currentTimeMillis() >= stopTime) { %>
                    <div class="alert alert-warning" role="alert">
                        <cpn:text class="text" i18n="true">Too many packages to display.</cpn:text>
                    </div>
                    <% } %>
                </c:if>
            </div>

        </div>
    </div>
    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
