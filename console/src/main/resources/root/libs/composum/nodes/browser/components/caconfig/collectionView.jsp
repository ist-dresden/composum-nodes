<%@ page import="java.io.PrintWriter" %>
<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
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
        <p>(TBD: lists the values for the items in this collection + inheritance setting and links to parents)</p>
        <c:if test="${model.thisCollectionConfiguration != null}">
            ${model.thisCollectionConfiguration.metadata.name} : ${model.thisCollectionConfiguration.metadata.description}
            <br/>
            <c:forEach var="item" items="${model.resource.childrenList}">
                <a href="/bin/browser.html/${item.path}">${item.name}</a><br/>
            </c:forEach>
            <c:forEach var="item" items="${model.thisCollectionConfiguration.configs}">
                <a href="/bin/browser.html/${model.path}/${item.name}">${item.name}</a>
                ${item.metadata.description}
            </c:forEach>
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
