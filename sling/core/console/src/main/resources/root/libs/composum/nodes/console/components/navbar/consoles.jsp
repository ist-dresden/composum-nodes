<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="status" type="com.composum.sling.nodes.console.Consoles">
    <ul class="nav navbar-nav">
        <c:forEach items="${status.consoles}" var="console">
            <li class="nav-item ${console.name} link"><a
                    href="${console.url}"${console.linkAttributes}>${console.label}</a></li>
        </c:forEach>
    </ul>
</cpn:component>
