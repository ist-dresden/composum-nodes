<%@ page import="java.io.PrintWriter" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model"
               type="com.composum.sling.nodes.components.CAConfigModel"
               scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>

    <div class="caconfig-toolbar detail-toolbar flex-toolbar">
        <div class="detail-headline" style="margin-right: auto; font-weight: bold;">Configuration Types</div>
        <div class="btn-group btn-group-sm" role="group">
            <button type="button" class="refresh fa fa-refresh btn btn-default"
                    title="${cpn:i18n(slingRequest,'Reload')}"><span
                    class="label">${cpn:i18n(slingRequest,'Reload')}</span>
            </button>
        </div>
    </div>

    <div class="detail-content">
        <%
            try {
        %>
        <table class="table table-striped">
            <c:if test="${not empty model.singletonConfigurations}">
                <tr>
                    <th></th>
                    <th colspan="2">Singleton Configurations</th>
                </tr>
                <c:forEach var="singletoncfg"
                           items="${model.singletonConfigurations}">
                    <tr>
                        <td>
                            <button type="button" class="create glyphicon-plus glyphicon btn btn-default"
                                    title="Create a new configuration"></button>
                        </td>
                        <td>
                            <a href="/bin/browser.html/${model.path}/${singletoncfg.metadata.name}">${singletoncfg.metadata.name}</a>
                        </td>
                        <td>
                                ${singletoncfg.metadata.description}
                        </td>
                    </tr>
                </c:forEach>
            </c:if>
            <c:if test="${not empty model.collectionConfigurations}">
            <tr>
                <th></th>
                <th colspan="2">Collection Configurations</th>
            </tr>
            <tbody>
            <c:forEach var="collectioncfg"
                    items="${model.collectionConfigurations}">
            <tr>
                <td>
                    <button type="button" class="create glyphicon-plus glyphicon btn btn-default"
                            title="Create a new configuration"></button>
                </td>
                <td>
                    <a href="/bin/browser.html/${model.path}/${collectioncfg.metadata.name}">${collectioncfg.metadata.name}</a>
                </td>
                <td>${collectioncfg.metadata.description}</td>
            </tr>
            </c:forEach>
            </c:if>
        </table>
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
</cpn:component>