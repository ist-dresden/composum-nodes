<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="label">${cpn:i18n(slingRequest,'Member of')}</div>
    <cpn:div test="${empty model.user.declaredModelOf}"
             class="empty-message">${cpn:i18n(slingRequest,'no relations configured')}</cpn:div>
    <cpn:div tagName="ul" test="${not empty model.user.declaredModelOf}" class="references">
        <c:forEach items="${model.user.declaredModelOf}" var="item">
            <li><a class="reference" href="#" data-path="${item.path}"><i
                    class="fa fa-${item.typeIcon}"></i>${item.id}</a></li>
        </c:forEach>
    </cpn:div>
</cpn:component>
