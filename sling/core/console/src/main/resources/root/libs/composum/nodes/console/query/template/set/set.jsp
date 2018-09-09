<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="templates" type="com.composum.sling.nodes.query.TemplateSet">
    <div class="query-templates">
        <ul class="nav nav-tabs" role="tablist">
            <c:forEach items="${templates.groups}" var="group">
                <li><a href="#query-template-group-${group.key}" role="tab"
                       data-toggle="tab"><cpn:text value="${group.key}"/></a></li>
            </c:forEach>
        </ul>
        <div class="tab-content">
            <c:forEach items="${templates.groups}" var="group">
                <div role="tabpanel" class="tab-pane" id="query-template-group-${group.key}">
                        <c:forEach items="${group.value}" var="template">
                            <sling:include path="${template.path}"/>
                        </c:forEach>
                </div>
            </c:forEach>
        </div>
    </div>
</cpn:component>