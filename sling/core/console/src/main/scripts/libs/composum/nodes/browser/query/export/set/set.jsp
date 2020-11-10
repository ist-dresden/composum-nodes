<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="set" type="com.composum.sling.nodes.query.ExportSet">
    <c:forEach items="${set.groups}" var="group">
        <li class="menu-item">
            <div class="menu-group">${cpn:i18n(slingRequest,group.key)}</div>
            <ul class="menu">
                <c:forEach items="${group.value}" var="strategy">
                    <li class="menu-item">
                        <sling:include path="${strategy.path}"/>
                    </li>
                </c:forEach>
            </ul>
        </li>
    </c:forEach>
</cpn:component>