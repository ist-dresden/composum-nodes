<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<c:forEach items="${consoleMenu.menuItems}" var="item">
    <c:choose>
        <c:when test="${item.menu}">
            <li class="menu-item ${item.name} menu${item.link.valid?' link':''}"><a
                    href="${item.url}"${item.linkAttributes}>${item.label}</a>
                <c:if test="${item.link.valid}"><a href="${item.link.url}" class="link fa fa-${item.link.icon}"
                                                   title="${item.link.title}" target="${item.link.target}"></a></c:if>
                <ul class="menu">
                    <c:set var="consoleMenu" value="${item}" scope="request"/>
                    <sling:call script="menu.jsp"/>
                </ul>
            </li>
        </c:when>
        <c:otherwise>
            <li class="${item.link.valid?'link':''}">
                <a class="menu-item ${item.name}" href="${item.url}"${item.linkAttributes}>${item.label}</a>
                <c:if test="${item.link.valid}"><a href="${item.link.url}" class="link fa fa-${item.link.icon}"
                                                   title="${item.link.title}" target="${item.link.target}"></a></c:if>
            </li>
        </c:otherwise>
    </c:choose>
</c:forEach>