<%@ page import="java.io.PrintWriter" %>
<%@ page import="org.apache.sling.api.resource.Resource" %>
<%@ page import="com.composum.sling.nodes.components.CAConfigModel" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<%!
    String renderAsString(Object object) {
        return CAConfigModel.renderValueAsString(object);
    }
%>
<cpn:component id="model" type="com.composum.sling.nodes.components.CAConfigModel" scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <c:set var="writeAllowed" value="${browser.permissible['nodes/repository/permissions']['write']}"/>
    <c:set var="writeDisabled" value="${writeAllowed?'':' disabled'}"/>

    <div class="caconfig-toolbar detail-toolbar flex-toolbar">
        <div class="detail-headline" style="margin-right: auto; font-weight: bold;">Configuration Collection</div>
        <div class="btn-group btn-group-sm" role="group">
            <button class="add fa fa-plus btn btn-default"${writeDisabled}
                    data-path="${model.path}" data-type="nt:unstructured"
                    title="${cpn:i18n(slingRequest,'Add new configuration')}">
                <span class="label">${cpn:i18n(slingRequest,'Add')}</span>
            </button>
            <button class="remove fa fa-minus btn btn-default"${writeDisabled}
                    title="${cpn:i18n(slingRequest,'Remove selected configurations')}"><span
                    class="label">${cpn:i18n(slingRequest,'Remove')}</span></button>
        </div>
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
        <c:if test="${model.thisCollectionConfiguration != null}">
            <cpn:text tagName="h4" value="${model.thisCollectionConfiguration.metadata.name}"/>
            <cpn:text tagName="p" value="${model.thisCollectionConfiguration.metadata.description}"/>
            <table class="table table-striped">
                <tr>
                    <th class="bs-checkbox"></th>
                    <th><cpn:text i18n="true">Name</cpn:text></th>
                    <th class="wide-column"><cpn:text i18n="true">Configured Values Overview</cpn:text></th>
                </tr>
                <c:forEach var="item" items="${model.resource.childrenList}">
                    <tr>
                        <td class="bs-checkbox"><input name="configresource" data-path="${model.path}"
                                                       data-name="${item.name}" type="radio"
                                                       class="selected-configuration"></td>
                        <td><a href="/bin/browser.html/${item.path}" data-path="${item.path}">${item.name}</a></td>
                        <td><%= renderAsString(((Resource) pageContext.getAttribute("item")).getValueMap()) %>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>

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

    <c:remove var="writeDisabled"/>
    <c:remove var="writeAllowed"/>
</cpn:component>
