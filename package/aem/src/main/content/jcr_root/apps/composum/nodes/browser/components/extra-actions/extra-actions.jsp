<%@page pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpn:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <li role="separator" class="divider"></li>
    <li>
        <a href="#" class="activate${browser.writeAllowed?'':' disabled'}"
           title="Activate the selected node">Activate</a>
    </li>
    <li>
        <a href="#" class="deactivate${browser.writeAllowed?'':' disabled'}"
           title="Deactivate the selected node">Deactivate</a>
    </li>
</cpn:component>