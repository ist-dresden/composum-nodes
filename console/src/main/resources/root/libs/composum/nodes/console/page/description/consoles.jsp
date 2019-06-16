<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model" type="com.composum.sling.nodes.console.Consoles">
    <div class="overview_list">
        <c:forEach items="${model.consoles}" var="console">
            <div>
                <c:if test="${not empty console.description}">
                    <sling:include path="${console.description}" replaceSelectors="short"/>
                </c:if>
            </div>
        </c:forEach>
    </div>
</cpn:component>
