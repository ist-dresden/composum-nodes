<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="genericView" type="com.composum.sling.nodes.browser.GenericView" scope="request">
    <a class="favorite-toggle fa fa-star-o" href="#" title="Tooggle favorite state"><span
            class="label">Favorite</span></a>
    <div class="node-tabs detail-tabs action-bar btn-toolbar" role="toolbar">
        <div class="btn-group btn-group-sm" role="group">
            <c:forEach items="${genericView.viewTabs}" var="genericTab">
                <a class="${genericTab.key} fa fa-${genericTab.icon} btn btn-default ${genericTab.css}"
                   href="#${genericTab.id}" data-group="${genericTab.group}"
                   title="${genericTab.title}"><span class="label">${genericTab.label}</span></a>
            </c:forEach>
        </div>
    </div>
    <div class="node-view-content detail-content">
    </div>
</cpn:component>
