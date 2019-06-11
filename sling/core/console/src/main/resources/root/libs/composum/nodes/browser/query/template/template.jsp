<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="template" type="com.composum.sling.nodes.query.Template">
    <div class="query-template panel panel-default" data-encoded="${template.data}">
        <div class="panel-heading" role="tab" id="template-head-${template.id}">
            <h4 class="panel-title">
                <a role="button" data-toggle="collapse" href="#template-body-${template.id}"
                   aria-controls="template-body-${template.id}"><cpn:text value="${template.title}"/></a>
            </h4>
        </div>
        <div id="template-body-${template.id}" class="panel-collapse collapse" role="tabpanel"
             aria-labelledby="template-head-${template.id}">
            <cpn:text tagName="div" class="panel-body" value="${template.description}" type="rich"/>
            <table class="table template-links">
                <c:if test="${not empty template.xpath}">
                    <tr>
                        <td>XPath</td>
                        <td width="100%"><a href="#" data-type="xpath">${cpn:text(template.xpath)}</a></td>
                    </tr>
                </c:if>
                <c:if test="${not empty template.sql2}">
                    <tr>
                        <td>SQL2</td>
                        <td width="100%"><a href="#" data-type="sql2">${cpn:text(template.sql2)}</a></td>
                    </tr>
                </c:if>
            </table>
        </div>
    </div>
</cpn:component>