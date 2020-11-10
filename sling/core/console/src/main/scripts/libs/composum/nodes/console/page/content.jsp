<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model" type="com.composum.sling.nodes.console.Consoles">
    <div class="content">
        <div class="row">
            <div class="left col col-xs-2">
                <ul class="nav nav-pills nav-stacked">
                    <li class="active"><a data-toggle="pill" href="#console-overview">Overview</a></li>
                    <c:forEach items="${model.consoles}" var="console">
                        <li><a data-toggle="pill" href="#console-${console.name}">${console.label}</a></li>
                    </c:forEach>
                </ul>
            </div>
            <div class="tab-content col col-xs-10">
                <div id="console-overview" class="tab-pane fade in active">
                    <sling:include resourceType="composum/nodes/console/page/description"/>
                </div>
                <c:forEach items="${model.consoles}" var="console">
                    <div id="console-${console.name}" class="tab-pane fade">
                        <c:if test="${not empty console.description}">
                            <sling:include path="${console.description}"/>
                        </c:if>
                    </div>
                </c:forEach>
            </div>
        </div>
    </div>
</cpn:component>
