<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.nodes.tools.OsgiBundlesModel" scope="request">
    <div class="content-wrapper">
        <div class="tools-osgi-bundles_header">
            <h1>OSGi Bundles <span class="total"></span><span class="active"></span><i></i></h1>
        </div>
        <table class="tools-osgi-bundles_table table">
        </table>
    </div>
</cpn:component>
