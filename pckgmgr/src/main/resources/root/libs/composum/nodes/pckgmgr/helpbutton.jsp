<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<div class="btn-group btn-group-sm" role="group">
    <a href="https://www.composum.com/home/nodes/pckgmgr.html" target="_blank">
        <button type="button" class="documentation fa fa-info btn btn-default"
                title="${cpn:i18n(slingRequest,'Package manager documentation')}"><span
                class="label">${cpn:i18n(slingRequest,'Documentation')}</span></button>
    </a>
</div>
