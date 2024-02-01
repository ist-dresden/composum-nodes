<%@ page import="java.io.PrintWriter" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%
    try {
%>
<cpn:component id="model"
               type="com.composum.sling.nodes.components.CAConfigModel"
               scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <c:set var="writeAllowed" value="${model.permissible['nodes/repository/permissions']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>

    <table class="table table-striped">
        <c:if test="${not empty model.singletonConfigurations}">
            <tr>
                <th></th>
                <th colspan="2" class="wide-column">Singleton Configurations</th>
            </tr>
            <c:forEach var="singletoncfg"
                       items="${model.singletonConfigurations}">
                <tr>
                    <c:choose>
                        <c:when test="${singletoncfg.resourceExists}">
                            <td>
                                    <%-- <button type="button" class="edit glyphicon-pencil glyphicon btn btn-default btn-xs"
                                            title="Edit the configuration"></button> --%>
                            </td>
                            <td>
                                <a class="target-link"
                                   data-path="${model.path}/${singletoncfg.metadata.name}">
                                        ${singletoncfg.metadata.name}</a>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                <button type="button"${writeDisabled}
                                        class="create glyphicon-plus glyphicon btn btn-default btn-xs create-configuration-resource"
                                        data-path="${model.path}" data-nodename="${singletoncfg.metadata.name}"
                                        data-type="nt:unstructured"
                                        title="Create a new configuration"></button>
                            </td>
                            <td>
                                    ${singletoncfg.metadata.name}
                            </td>
                        </c:otherwise>
                    </c:choose>
                    <td>
                            ${singletoncfg.metadata.description}
                    </td>
                </tr>
            </c:forEach>
        </c:if>
        <c:if test="${not empty model.collectionConfigurations}">
        <tr>
            <th></th>
            <th colspan="2" class="wide-column">Collection Configurations</th>
        </tr>
        <tbody>
        <c:forEach var="collectioncfg"
                items="${model.collectionConfigurations}">
        <tr>
            <c:choose>
                <c:when test="${collectioncfg.resourceExists}">
                    <td>
                    </td>
                    <td>
                        <a class="target-link"
                           data-path="${model.path}/${collectioncfg.metadata.name}">
                                ${collectioncfg.metadata.name}</a>
                    </td>
                </c:when>
                <c:otherwise>
                    <td>
                        <button type="button"${writeDisabled}
                                class="create glyphicon-plus glyphicon btn btn-default btn-xs create-configuration-collection-resource"
                                data-path="${model.path}" data-nodename="${collectioncfg.metadata.name}"
                                data-type="sling:Folder"
                                title="Create a new configuration"></button>
                    </td>
                    <td>
                            ${collectioncfg.metadata.name}
                    </td>
                </c:otherwise>
            </c:choose>
            <td>${collectioncfg.metadata.description}</td>
        </tr>
        </c:forEach>
        </c:if>
    </table>

    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
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
