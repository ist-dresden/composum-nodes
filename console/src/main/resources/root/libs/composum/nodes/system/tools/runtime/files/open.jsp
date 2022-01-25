<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="btn-group">
    <a href="#" target="_blank" class="open fa fa-external-link btn btn-default"
       title="${cpn:i18n(slingRequest,'Open separated view')}"></a>
</div>