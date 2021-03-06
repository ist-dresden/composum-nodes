<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div class="breadcrumbs">
        <button class="breadcrumbs-toggle btn btn-default btn-sm fa fa-terminal"></button>
        <div class="path-input-group input-group">
            <input class="path-input-field form-control" type="text" value="${browser.current.path}"
                   placeholder="${cpn:i18n(slingRequest,'repository path')}"/>
            <span class="input-group-addon open-path" title="${cpn:i18n(slingRequest, 'Go to the path...')}">...</span>
        </div>
        <ol class="breadcrumbs-list">
            <c:forEach var="parent" items="${browser.parents}">
                <li data-path="${parent.path}"><cpn:link
                        href="/bin/browser.html${parent.path}">${parent.nameEscaped}</cpn:link></li>
            </c:forEach>
            <li class="active" data-path="${browser.current.path}"><cpn:link
                    href="/bin/browser.html${browser.current.path}">${browser.current.nameEscaped}</cpn:link></li>
        </ol>
        <cpn:div test="${not empty browser.relatedPathSet}" class="related-path-set btn-group btn-group-sm dropdown">
            <c:forEach var="type" items="${browser.relatedPathSet}">
                <button class="related-path btn btn-default btn-sm ${browser.path==type.value.path?'active':''} ${type.value.actions}"
                        data-path="${type.value.path}" title="${type.value.tooltip}">${type.value.label}</button>
            </c:forEach>
            <c:if test="${not empty browser.supertypeChain}">
                <button type="button" class="supertypes btn btn-default dropdown-toggle"
                        data-toggle="dropdown" aria-expanded="false"
                        title="${cpn:i18n(slingRequest, 'Supertypes Chain (Menu)')}">S
                </button>
                <ul class="supertypes-menu dropdown-menu dropdown-menu-right" role="menu">
                    <c:forEach var="supertype" items="${browser.supertypeChain}">
                        <li><a href="#" data-path="${supertype}">${supertype}</a></li>
                    </c:forEach>
                </ul>
            </c:if>
        </cpn:div>
    </div>
</cpn:component>
