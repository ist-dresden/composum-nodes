<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <ul class="status-block indicator-block ${pckg.cssClasses}">
        <li class="status-indicator install-status"><span class="is-installed">installed</span><span class="not-installed">not installed</span></li>
        <li class="status-indicator sealed-status"><span class="is-sealed">sealed</span><span class="not-sealed">not sealed</span></li>
        <li class="status-indicator valid-status"><span class="is-valid">valid</span><span class="not-valid">invalid !</span></li>
    </ul>
</cpn:component>