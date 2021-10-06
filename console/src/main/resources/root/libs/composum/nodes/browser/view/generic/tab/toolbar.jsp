<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="genericView" type="com.composum.sling.nodes.browser.GenericView" scope="request">
    <div class="${genericView.id}-toolbar detail-toolbar">
        <c:forEach items="${genericView.toolbar.groups}" var="genericGroup">
            <div class="btn-group btn-group-sm" role="group">
                <c:forEach items="${genericGroup.elements}" var="groupElement">
                    <c:set var="genericElement" value="${groupElement}" scope="request"/>
                    <sling:call script="${groupElement.type}.jsp"/>
                    <c:remove var="genericElement"/>
                </c:forEach>
            </div>
        </c:forEach>
    </div>
</cpn:component>