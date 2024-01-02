<%@ page import="java.io.PrintWriter" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%
    try {
%>
<h4>Configuration types list</h4>
<p>(TBD: lists all configurations types incl. descriptions and collection / single; buttons to create configurations /
    configuration collections)</p>
<cpn:component id="model" type="com.composum.sling.nodes.components.CAConfigModel" scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <c:if test="${not empty model.singletonConfigurations}">
        <h5>Singleton Configurations</h5>
        <c:forEach var="singletoncfg" items="${model.singletonConfigurations}">
            ${singletoncfg.metadata.name} : ${singletoncfg.metadata.description}
        </c:forEach>
    </c:if>
    <c:if test="${not empty model.collectionConfigurations}">
        <h5>Collection Configurations</h5>
        <c:forEach var="collectioncfg" items="${model.collectionConfigurations}">
            ${collectioncfg.metadata.name} : ${collectioncfg.metadata.description}
        </c:forEach>
    </c:if>
</cpn:component>
<%
    } catch (Exception ex) {
        log.error(ex.toString(), ex);
        PrintWriter writer = response.getWriter();
        writer.println("<pre>");
        ex.printStackTrace(writer);
        writer.println("</pre>");
    }
%>
