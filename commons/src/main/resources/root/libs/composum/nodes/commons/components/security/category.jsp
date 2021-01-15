<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.security.SetupConfiguration">
    <form class="composum-nodes-security-config_category widget-form">
        <h4 class="categrories-title title">${cpn:i18n(slingRequest, 'Category')}</h4>
        <div class="categories-list">
            <c:forEach items="${model.categories}" var="category">
                <div class="category-widget">
                    <input type="checkbox" name="category" value="${category}"
                           class="category-select"/>
                    <cpn:text class="category-key">${category}</cpn:text>
                </div>
            </c:forEach>
        </div>
    </form>
</cpn:component>
