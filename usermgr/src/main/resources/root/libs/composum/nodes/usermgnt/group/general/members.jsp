<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.Group" scope="request">
    <div class="label">${cpn:i18n(slingRequest,'Members')}</div>
    <cpn:div test="${empty model.group.declaredGroups}"
             class="empty-message">${cpn:i18n(slingRequest,'no Group members configured')}</cpn:div>
    <cpn:div tagName="ul" test="${not empty model.group.declaredGroups}" class="references">
        <c:forEach items="${model.group.declaredGroups}" var="item">
            <li><a class="reference" href="#" data-path="${item.path}"><i
                    class="fa fa-${item.typeIcon}"></i>${item.id}</a></li>
        </c:forEach>
    </cpn:div>
    <cpn:div test="${empty model.group.declaredUsers}"
             class="empty-message">${cpn:i18n(slingRequest,'no User members configured')}</cpn:div>
    <cpn:div tagName="ul" test="${not empty model.group.declaredUsers}" class="references">
        <c:forEach items="${model.group.declaredUsers}" var="item">
            <li><a class="reference" href="#" data-path="${item.path}"><i
                    class="fa fa-${item.typeIcon}"></i>${item.id}</a></li>
        </c:forEach>
    </cpn:div>
</cpn:component>
