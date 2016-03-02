<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.PackageBean" scope="request">
    <div class="detail-panel filters ${pckg.cssClasses}">
        <div class="filters-toolbar">
        </div>
        <div class="filters-content">
        </div>
    </div>
</cpn:component>