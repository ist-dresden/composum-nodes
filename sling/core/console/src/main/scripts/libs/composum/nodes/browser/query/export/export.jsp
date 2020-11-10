<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="strategy" type="com.composum.sling.nodes.query.ExportCfg">
    <form class="query-export-form" target="_blank"
          action="/bin/cpm/nodes/node.query${strategy.selectors}.bin${strategy.filename}" method="POST">
        <input type="hidden" name="export" value="${strategy.exportType}"/>
        <input type="hidden" name="query" value="${strategy.query}"/>
        <input type="hidden" name="filter" value="${strategy.filter}"/>
        <a class="query-export-link" href="#"
           title="${cpn:i18n(slingRequest,strategy.description)}">${cpn:i18n(slingRequest,strategy.title)}</a>
    </form>
</cpn:component>