<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.apache.sling.caconfig.management.ValueInfo" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%!
    String renderAsStringOrArray(Object valueInfo) {
        Object object = ((ValueInfo<?>) valueInfo).getEffectiveValue();
        if (Object[].class.isAssignableFrom(object.getClass())) {
            StringBuilder builder = new StringBuilder();
            for (Object item : (Object[]) object) {
                if (builder.length() > 0) {
                    builder.append("<br/>");
                }
                builder.append(item);
            }
            return builder.toString();
        } else {
            return object.toString();
        }
    }
%>

<cpn:component id="model" type="com.composum.sling.nodes.components.CAConfigModel" scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <c:set var="config" value="${model.thisSingletonConfiguration}"/>

    <div class="caconfig-toolbar detail-toolbar flex-toolbar">
        <div class="detail-headline" style="margin-right: auto; font-weight: bold;">Configuration Edit</div>
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
        <p>(TBD: lists the values this configuration + inheritance settings, possibly links to the parents, everything
            editable)</p>
        <h4>${config.metadata.name}</h4>
            ${config.metadata.description}
        <br/>
        <table class="table table-striped">
            <thead>
            <tr>
                <th>Property</th>
                <th>Label</th>
                <th></th>
                <th></th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="propInfo" items="${config.valueInfos}">
                <tr>
                    <th scope="row">${propInfo.name}</th>
                    <td title="${propInfo.propertyMetadata.description}">
                            ${propInfo.propertyMetadata.label}
                    </td>
                    <td>
                        <c:if test="${not empty propInfo.propertyMetadata.description}">
                        <span class="fa fa-info-circle"
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
                        <%= renderAsStringOrArray(pageContext.getAttribute("propInfo")) %>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <%
        } catch (Exception ex) {
            log.error(ex.toString(), ex);
            PrintWriter writer = response.getWriter();
            writer.println("<pre>");
            ex.printStackTrace(writer);
            writer.println("</pre>");
        }
    %>
</cpn:component>
