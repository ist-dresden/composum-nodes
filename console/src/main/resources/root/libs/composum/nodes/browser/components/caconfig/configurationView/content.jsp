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
    <p>Please click on the line in the table to edit the value.</p>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th>Property</th>
            <th>Label</th>
            <th></th>
            <th></th>
            <th></th>
            <th class="valuecolumn">Value</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="propInfo" items="${config.propertyInfos}">
            <tr title="${propInfo.valueInfo.propertyMetadata.description}" class="caconfig-property-editor"
                data-multi="${propInfo.multiValue}" data-typename="${propInfo.typeName}"
                data-path="${model.path}" data-propertyname="${propInfo.name}"
                data-value='${cpn:value(propInfo.jsonValue)}' data-description="${propInfo.valueInfo.propertyMetadata.description}"
                data-default="${propInfo.valueInfo.default}" data-required="${propInfo.required}" data-properties="${propInfo.properties}">
                <th scope="row">${propInfo.name}</th>
                <td>
                        ${propInfo.valueInfo.propertyMetadata.label}
                </td>
                <td>
                    <%-- <cpn:text value="${propInfo.propertyMetadata.description}"/> --%>
                    <c:if test="${not empty propInfo.valueInfo.propertyMetadata.description}">
                        <span class="fa fa-info-circle infosymbol" data-toggle="tooltip"
                              title="${propInfo.valueInfo.propertyMetadata.description}">
                        </span>
                    </c:if>
                </td>
                <td>
                    <c:if test="${propInfo.required}">
                        <span class="fa fa-asterisk infosymbol" data-toggle="tooltip"
                              title="This is a required configuration property.">
                        </span>
                    </c:if>
                </td>
                <td>
                    <c:if test="${not empty propInfo.properties}">
                        <span class="fa fa-product-hunt infosymbol" data-toggle="tooltip"
                              title="Properties: ${propInfo.properties}">
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
