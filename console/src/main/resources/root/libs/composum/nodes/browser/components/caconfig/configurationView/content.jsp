<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.apache.sling.caconfig.management.ValueInfo" %>
<%@ page import="com.composum.sling.nodes.components.CAConfigModel" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%!
    String renderValue(Object rawModel, Object valueInfoObject) {
        ValueInfo valueInfo = (ValueInfo) valueInfoObject;
        CAConfigModel model = (CAConfigModel) rawModel;
        Object value = model.getResource().getValueMap().get(valueInfo.getName());
        return CAConfigModel.renderValueAsString(value);
    }
%>
<%
    try {
%>

<cpn:component id="model" type="com.composum.sling.nodes.components.CAConfigModel" scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <c:set var="config" value="${model.thisSingletonConfiguration}"/>
    <cpn:text tagName="h4" value="${config.metadata.name}"/>
    <cpn:text tagName="p" value="${config.metadata.description}"/>
    <p>Please click on the line in the table to edit the value. The description of the item is shown on mouse hover.</p>
    <table class="table table-striped">
        <thead>
        <tr>
            <th>Property</th>
            <th>Label</th>
            <th></th>
            <th>Value</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="propInfo" items="${config.valueInfos}">
            <tr title="${propInfo.propertyMetadata.description}">
                <th scope="row">${propInfo.name}</th>
                <td>
                        ${propInfo.propertyMetadata.label}
                </td>
                <td>
                        <%-- <cpn:text value="${propInfo.propertyMetadata.description}"/> --%>
                    <c:if test="${not empty propInfo.propertyMetadata.description}">
                        <span class="fa fa-info-circle"
                              title="${propInfo.propertyMetadata.description}">
                        </span>
                    </c:if>
                </td>
                <td class="${propInfo.default ? 'text-muted' : ''}">
                    <%= renderValue(request.getAttribute("model"), pageContext.getAttribute("propInfo")) %>
                </td>
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
