<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<c:forEach items="${consoleMenu.menuItems}" var="item">
    <c:choose>
        <c:when test="${item.menu}">
            <li class="menu-item ${item.name} menu"><a href="${item.url}"${item.linkAttributes}>${item.label}</a>
                <ul class="menu">
                    <c:set var="consoleMenu" value="${item}" scope="request"/>
                    <sling:call script="menu.jsp"/>
                </ul>
            </li>
        </c:when>
        <c:otherwise>
            <li><a class="menu-item ${item.name}" href="${item.url}"${item.linkAttributes}>${item.label}</a></li>
        </c:otherwise>
    </c:choose>
</c:forEach>