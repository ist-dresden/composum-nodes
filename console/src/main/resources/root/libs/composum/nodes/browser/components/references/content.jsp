<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model" type="com.composum.sling.nodes.components.ReferencesModel" scope="request">
    <c:choose>
        <c:when test="${model.hasHits}">
            <ul class="references-list">
                <c:forEach items="${model.hits}" var="hit">
                    <li class="references-list_hit">
                        <a class="references-list_hit_toggle" href="#"></a>
                        <a class="references-list_hit_link" href="#"
                           data-path="${hit.resource.path}"><span
                                class="references-list_hit_link-label">${cpn:text(hit.resource.path)}</span></a>
                        <ul class="references-list_hit_properties">
                            <c:forEach items="${hit.properties}" var="property">
                                <li class="references-list_hit_property">
                                    <div class="references-list_hit_property-name">${property.name}</div>
                                    <c:choose>
                                        <c:when test="${property.multi}">
                                            <ul class="references-list_hit_property_values">
                                                <c:forEach items="${property.values}" var="value">
                                                    <li class="references-list_hit_property_value">
                                                        <cpn:text class="references-list_hit_property_value-text"
                                                                  value="${value.text}"
                                                                  type="${value.richText?'rich':'text'}"/>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                        </c:when>
                                        <c:otherwise>
                                            <cpn:text class="references-list_hit_property_value-text"
                                                      value="${property.text}"
                                                      type="${property.richText?'rich':'text'}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                        </ul>
                    </li>
                </c:forEach>
            </ul>
        </c:when>
        <c:otherwise>
            <cpn:text class="references-message" i18n="true"
                      value="${empty model.message?'no references found':model.message}"/>
            <cpn:text class="references-query-string" value="${model.queryString}"/>
            <pre class="references-options"><code>${model.optionsJson}</code></pre>
        </c:otherwise>
    </c:choose>
</cpn:component>
