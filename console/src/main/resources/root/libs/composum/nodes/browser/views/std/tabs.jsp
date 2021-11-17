<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <a class="source json fa fa-code btn btn-default" href="#json" data-group="json"
       title="Source view as JSON (switchable to XML)"><span class="label">JSON</span></a>
    <a class="source xml fa fa-code btn btn-default hidden" href="#xml" data-group="xml"
       title="Source view as XML (switchable to JSON)"><span class="label">XML</span></a>
    <a class="references fa fa-crosshairs btn btn-default" href="#references"
       data-group="references"
       title="${cpn:i18n(slingRequest,'References')}"><span
            class="label">${cpn:i18n(slingRequest,'References')}</span></a>
    <c:if test="${browser.permissible['nodes/repository/permissions']['read'] && browser.canHaveAcl}">
        <a class="acl fa fa-key btn btn-default" href="#acl" data-group="acl" title="Access Rules"><span
                class="label">ACL</span></a>
    </c:if>
    <c:if test="${browser.permissible['nodes/repository/versions']['read'] && browser.versionable}">
        <a class="version fa fa-history btn btn-default" href="#version" data-group="version"
           title="Versions"><span class="label">Versions</span></a>
    </c:if>
</cpn:component>
