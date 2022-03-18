<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="status" type="com.composum.sling.nodes.console.ConsolesModel">
    <ul class="nav navbar-nav">
        <c:forEach items="${status.consoles}" var="console">
            <c:choose>
                <c:when test="${console.menu}">
                    <c:if test="${console.validMenu}">
                        <li class="nav-item ${console.name} dropdown"><a
                                href="${console.url}"${console.linkAttributes}>${console.label}</a><a
                                href="#" class="dropdown-toggle" data-toggle="dropdown" role="button"
                                aria-haspopup="true" aria-expanded="false"><span class="caret"></span></a>
                            <ul class="dropdown-menu">
                                <c:set var="consoleMenu" value="${console}" scope="request"/>
                                <sling:call script="menu.jsp"/>
                                <c:remove var="consoleMenu"/>
                            </ul>
                        </li>
                    </c:if>
                </c:when>
                <c:otherwise>
                    <li class="nav-item ${console.name} link"><a
                            href="${console.url}"${console.linkAttributes}>${console.label}</a></li>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </ul>
</cpn:component>
