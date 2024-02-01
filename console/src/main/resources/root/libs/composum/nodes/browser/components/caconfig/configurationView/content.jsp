<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.apache.sling.caconfig.management.ValueInfo" %>
<%@ page import="com.composum.sling.nodes.components.CAConfigModel" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%
    try {
%>

<cpn:component id="model" type="com.composum.sling.nodes.components.CAConfigModel" scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <c:set var="config" value="${model.thisSingletonConfiguration}"/>
    <cpn:text tagName="h4" value="${config.metadata.name}"/>
    <cpn:text tagName="p" value="${config.metadata.description}"/>
    <p>Please click on the line in the table to edit the value. The description of the item is shown on mouse hover.</p>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th>Property</th>
            <th>Label</th>
            <th></th>
            <th>Value</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="propInfo" items="${config.propertyInfos}">
            <tr title="${propInfo.metadata.propertyMetadata.description}" class="caconfig-property-editor"
                data-ismulti="${propInfo.multiValue}" data-typename="${propInfo.typeName}"
                data-path="${model.path}" data-propertyname="${propInfo.name}"
                data-value="${propInfo.renderedValue}">
                <th scope="row">${propInfo.name}</th>
                <td>
                        ${propInfo.metadata.propertyMetadata.label}
                </td>
                <td>
                    <%-- <cpn:text value="${propInfo.propertyMetadata.description}"/> --%>
                    <c:if test="${not empty propInfo.metadata.propertyMetadata.description}">
                        <span class="fa fa-info-circle"
                              title="${propInfo.metadata.propertyMetadata.description}">
                        </span>
                    </c:if>
                </td>
                <td class="${propInfo.valueInfo.default ? 'text-muted' : ''}">${propInfo.renderedValue}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <c:remove var="config"/>
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
