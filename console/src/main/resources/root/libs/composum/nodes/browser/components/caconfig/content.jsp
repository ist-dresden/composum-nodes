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
    <%--
    <ul>
        <c:forEach var="meta" items="${model.allMetaData}">
            <li>
                    ${meta.label} : ${meta.description}
                <ul>
                    <c:forEach var="prop" items="${meta.propertyMetadata}">
                        <li>
                                ${prop.key} : ${prop.value}
                        </li>
                    </c:forEach>
                </ul>
                <ul>
                    <c:forEach var="propMeta" items="${meta.properties}">
                        <li>
                                ${propMeta.key} : ${propMeta.value}
                        </li>
                    </c:forEach>
                </ul>
            </li>
        </c:forEach>
    </ul>
    --%>
    <c:forEach var="config" items="${model.singletonConfigurations}">
        <%--@elvariable id="config" type="com.composum.sling.nodes.components.CAConfigModel.SingletonConfigInfo"--%>
        <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">${config.metadata.label} ( ${config.name} )</h3>
        </div>
        <div class="panel-body">
            <div class="row">
                <div class="col-md-12">
                    <p>${config.metadata.description}</p>
                    <p>Configuration location: ${config.configurationData.resourcePath}</p>
                </div>
            </div>
            <div class="row">
                <div class="col-md-12">
                    <dl>
                        <c:forEach var="propInfo" items="${config.valueInfos}">
                            <dt>${propInfo.propertyMetadata.label} = ${propInfo.value}</dt>
                            <dd>
                                <dl>
                                    <dt>description</dt>
                                    <dd>${propInfo.propertyMetadata.description}</dd>
                                    <dt>props</dt>
                                    <dd>${propInfo.propertyMetadata.properties}</dd>
                                    <dt>default</dt>
                                    <dd>${propInfo.propertyMetadata.defaultValue}</dd>
                                    <dt>default2</dt>
                                    <dd>${propInfo.default}</dd>
                                    <dt>cfgsrc</dt>
                                    <dd>${propInfo.configSourcePath}</dd>
                                    <dt>effval</dt>
                                    <dd>${propInfo.effectiveValue}</dd>
                                    <dt>inh</dt>
                                    <dd>${propInfo.inherited}</dd>
                                    <dt>overr</dt>
                                    <dd>${propInfo.overridden}</dd>
                                    <dt>name</dt>
                                    <dd>${propInfo.name}</dd>
                                </dl>
                            </dd>
                        </c:forEach>
                    </dl>
                </div>
            </div>
        </div>
        <hr>
    </c:forEach>

    <c:forEach var="collection" items="${model.collectionConfigurations}">
        <%--@elvariable id="collection" type="com.composum.sling.nodes.components.CAConfigModel.CollectionConfigInfo"--%>
        <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">${collection.metadata.label} ( ${collection.collectionConfigData.configName} )</h3>
        </div>
        <div class="panel-body">
        <div class="row">
            <div class="col-md-12">
                <p>${collection.metadata.description}</p>
            </div>
        </div>
        <div class="row">
            <div class="col-md-12">
                <c:forEach var="config"
                           items="${collection.configs}">
                    <%--@elvariable id="config" type="com.composum.sling.nodes.components.CAConfigModel.SingletonConfigInfo"--%>
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            <h5 class="panel-title">
                                    ${config.configurationData.collectionItemName}
                            </h5>
                        </div>
                        <div class="row">
                            <div class="col-md-12">
                                <p>from ${config.configurationData.resourcePath}</p>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-12">
                                <c:forEach var="propInfo" items="${config.valueInfos}">
                                    <dt>${propInfo.propertyMetadata.label} = ${propInfo.value}</dt>
                                    <dd>
                                        <dl>
                                            <dt>description</dt>
                                            <dd>${propInfo.propertyMetadata.description}</dd>
                                            <dt>props</dt>
                                            <dd>${propInfo.propertyMetadata.properties}</dd>
                                            <dt>default</dt>
                                            <dd>${propInfo.propertyMetadata.defaultValue}</dd>
                                            <dt>default2</dt>
                                            <dd>${propInfo.default}</dd>
                                            <dt>cfgsrc</dt>
                                            <dd>${propInfo.configSourcePath}</dd>
                                            <dt>effval</dt>
                                            <dd>${propInfo.effectiveValue}</dd>
                                            <dt>inh</dt>
                                            <dd>${propInfo.inherited}</dd>
                                            <dt>overr</dt>
                                            <dd>${propInfo.overridden}</dd>
                                            <dt>name</dt>
                                            <dd>${propInfo.name}</dd>
                                        </dl>
                                    </dd>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </c:forEach>
</cpn:component>
<%
    } catch (Exception ex) {
        ex.printStackTrace(response.getWriter());
    }
%>
