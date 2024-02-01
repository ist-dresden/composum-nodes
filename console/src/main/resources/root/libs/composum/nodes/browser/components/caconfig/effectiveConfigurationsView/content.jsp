<%@ page import="java.io.PrintWriter" %>
<%@ page import="com.composum.sling.nodes.components.CAConfigModel" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%!
    String renderAsString(Object valueInfo) {
        return CAConfigModel.renderValueInfoAsString(valueInfo);
    }
%>
<%
    try {
%>
<cpn:component id="model"
               type="com.composum.sling.nodes.components.CAConfigModel"
               scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>

    <p>To edit the configuration go to the mentioned configuration locations.
        To create a new configuration you can go to the following nodes:
    </p>
    <ul>
        <c:forEach var="configPath" items="${model.referencedConfigPaths}">
            <li>
                <a class="target-link"
                   href="/bin/browser.html${configPath}"
                   data-path="${configPath}">${configPath}</a>
            </li>
        </c:forEach>
    </ul>
    <c:forEach var="config"
               items="${model.singletonConfigurations}">
        <%--@elvariable id="config" type="com.composum.sling.nodes.components.CAConfigModel.SingletonConfigInfo"--%>
        <div class="panel panel-default">
            <div class="panel-heading">
                <h3 class="panel-title">${config.metadata.label} ( ${config.name} )
                        <%-- <a class="editable pull-right"><span class="target-link btn btn-default btn-xs fa fa-share"
                                                             data-path="${config.configurationData.resourcePath}"
                                                             title="Configuration location: ${config.configurationData.resourcePath}"></span></a> --%>
                </h3>
            </div>
            <div class="panel-body">
                <p>${config.metadata.description}</p>
                <p>Configuration location:
                    <c:choose>
                        <c:when test="${not empty config.configurationData.resourcePath}">
                            <a class="target-link"
                               href="/bin/browser.html${config.configurationData.resourcePath}"
                               data-path="${config.configurationData.resourcePath}">${config.configurationData.resourcePath}</a>
                            , ${config.inherits ? 'inherits configurations' : 'does not inherit configurations'}
                        </c:when>
                        <c:otherwise>
                            (defaults)
                        </c:otherwise>
                    </c:choose>
                </p>
                <table class="table table-striped">
                    <thead>
                    <tr>
                        <th>Property</th>
                        <th>Label</th>
                        <th></th>
                        <th></th>
                        <th class="valuecolumn">Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="propInfo" items="${config.valueInfos}">
                        <tr>
                            <th scope="row">${propInfo.name} (${propInfo.propertyMetadata.type.simpleName})</th>
                            <td title="${propInfo.propertyMetadata.description}">
                                    ${propInfo.propertyMetadata.label}
                            </td>
                            <td>
                                <c:if test="${not empty propInfo.propertyMetadata.description}">
                                        <span class="fa fa-info-circle" data-toggle="tooltip"
                                              title="${propInfo.propertyMetadata.description}">
                                        </span>
                                </c:if>
                            </td>
                            <td>
                                <c:if test="${propInfo.inherited}">
                                    <a class="target-link btn btn-default btn-xs fa fa-share"
                                       data-path="${propInfo.configSourcePath}"
                                       href="/bin/browser.html${propInfo.configSourcePath}"
                                       title="Configuration inherited from: ${propInfo.configSourcePath}"></a>
                                </c:if>
                            </td>
                            <td class="${propInfo.default ? 'text-muted' : ''}">
                                <%= renderAsString(pageContext.getAttribute("propInfo")) %>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </c:forEach>

    <hr>

    <c:forEach var="collection"
               items="${model.collectionConfigurations}">
        <%--@elvariable id="collection" type="com.composum.sling.nodes.components.CAConfigModel.CollectionConfigInfo"--%>
        <div class="panel panel-default">
            <div class="panel-heading">
                <h3 class="panel-title">${collection.metadata.label}
                    ( ${collection.collectionConfigData.configName}
                    )</h3>
            </div>
            <div class="panel-body">
                <p>${collection.metadata.description}</p>
                <p>Collection location:
                    <c:choose>
                        <c:when test="${not empty collection.collectionConfigData.resourcePath}">
                            <a class="target-link"
                               href="/bin/browser.html${collection.collectionConfigData.resourcePath}"
                               data-path="${collection.collectionConfigData.resourcePath}">${collection.collectionConfigData.resourcePath}</a>
                            , ${collection.inherits ? 'inherits configurations' : 'does not inherit configurations'}
                        </c:when>
                        <c:otherwise>
                            (defaults)
                        </c:otherwise>
                    </c:choose>
                </p>
                <c:forEach var="config"
                           items="${collection.configs}">
                    <%--@elvariable id="config" type="com.composum.sling.nodes.components.CAConfigModel.SingletonConfigInfo"--%>
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h5 class="panel-title">
                                    ${config.configurationData.collectionItemName}
                                <c:if test="${config.configurationData.inherited}">(inherited)</c:if>
                            </h5>
                        </div>
                        <div class="panel-body">
                            <p>Configuration location:
                                <c:choose>
                                    <c:when test="${not empty config.configurationData.resourcePath}">
                                        <a class="target-link"
                                           href="/bin/browser.html${config.configurationData.resourcePath}"
                                           data-path="${config.configurationData.resourcePath}">${config.configurationData.resourcePath}</a>
                                    </c:when>
                                    <c:otherwise>
                                        (defaults)
                                    </c:otherwise>
                                </c:choose>
                            </p>
                            <!-- bootstrap table striped -->
                            <table class="table table-striped">
                                <thead>
                                <tr>
                                    <th>Property</th>
                                    <th>Label</th>
                                    <th></th>
                                    <th></th>
                                    <th class="valuecolumn">Value</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="propInfo" items="${config.valueInfos}">
                                    <tr>
                                        <th scope="row">${propInfo.name} (${propInfo.propertyMetadata.type.simpleName})</th>
                                        <td title="${propInfo.propertyMetadata.description}">
                                                ${propInfo.propertyMetadata.label}
                                        </td>
                                        <td>
                                            <c:if test="${not empty propInfo.propertyMetadata.description}">
                                                    <span class="fa fa-info-circle" data-toggle="tooltip"
                                                          title="${propInfo.propertyMetadata.description}">
                                                    </span>
                                            </c:if>
                                        </td>
                                        <td>
                                            <c:if test="${propInfo.inherited && propInfo.configSourcePath != config.configurationData.resourcePath}">
                                                <a class="target-link btn btn-default btn-xs fa fa-share"
                                                   data-path="${propInfo.configSourcePath}"
                                                   href="/bin/browser.html${propInfo.configSourcePath}"
                                                   title="Configuration inherited from: ${propInfo.configSourcePath}"></a>
                                            </c:if>
                                        </td>
                                        <td class="${propInfo.default ? 'text-muted' : ''}">
                                            <%= renderAsString(pageContext.getAttribute("propInfo")) %>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </c:forEach>
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
