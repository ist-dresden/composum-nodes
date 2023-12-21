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
    <dl>
        <c:forEach var="config" items="${model.singletonConfigurations}">
            <%--@elvariable id="config" type="com.composum.sling.nodes.components.CAConfigModel.SingletonConfigInfo"--%>
            <dt>${config.metadata.label} ( ${config.name} ) : ${config.metadata.description}
                    ${config.configurationData.resourcePath}
            </dt>
            <dd>
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
            </dd>
        </c:forEach>
    </dl>
</cpn:component>
<%
    } catch (Exception ex) {
        ex.printStackTrace(response.getWriter());
    }
%>
