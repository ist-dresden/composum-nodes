<%@ page import="java.io.PrintWriter" %>
<%@ page import="com.composum.sling.nodes.components.CAConfigModel" %>
<%@ page import="org.apache.sling.api.resource.Resource" %>
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

    <div class="caconfig-toolbar detail-toolbar flex-toolbar">
        <div class="detail-headline" style="margin-right: auto; font-weight: bold;">Configuration Collection</div>
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
                <c:forEach var="item" items="${model.resource.childrenList}">
                    <tr>
                        <td><a href="/bin/browser.html/${item.path}" data-path="${item.path}">${item.name}</a></td>
                        <td><%= renderAsString(((Resource) pageContext.getAttribute("item")).getValueMap()) %></td>
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

</cpn:component>
