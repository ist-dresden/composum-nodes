<%@ page import="java.io.PrintWriter" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<div class="detail-content">
    <%
        try {
    %>
    <h4>Configuration Collection</h4>
    <p>(TBD: lists the values for the items in this collection + inheritance setting and links to parents)</p>
    <cpn:component id="model" type="com.composum.sling.nodes.components.CAConfigModel" scope="request">
        <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
        <c:if test="${model.thisCollectionConfiguration != null}">
            ${model.thisCollectionConfiguration.metadata.name} : ${model.thisCollectionConfiguration.metadata.description}
            <br/>
            <c:forEach var="item" items="${model.resource.childrenList}">
                <a href="/bin/browser.html/${item.path}">${item.name}</a><br/>
            </c:forEach>
            <c:forEach var="item" items="${model.thisCollectionConfiguration.configs}">
                <a href="/bin/browser.html/${model.path}/${item.name}">${item.name}</a>
                ${item.metadata.description}
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
</div>
