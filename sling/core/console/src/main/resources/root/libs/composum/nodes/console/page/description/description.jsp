<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model" type="com.composum.sling.nodes.console.Consoles">
    <div class="description">
        <cpn:link href="/libs/composum/nodes/console/content.html"><h3>Overview</h3></cpn:link>
        <p>The Console is offering a set of tools for the platform management and system administration.
            In comparision to the 'Apache Sling Web Console' is this set of tools intended to use by normal,
            non administrative users.
            The set of tools available for the user is controlled by the access rules of the repository.
            The content within the available tools can also vary according to the rights of the current user.</p>
        <div class="overview">
            <h4 class="section">Available Tools</h4>
            <sling:call script="consoles.jsp"/>
        </div>
    </div>
</cpn:component>
