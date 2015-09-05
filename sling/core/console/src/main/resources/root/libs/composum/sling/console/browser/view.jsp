<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
    <sling:call script="/libs/composum/sling/console/browser/breadcrumbs.jsp" />
    <sling:call script="/libs/composum/sling/console/browser/views/${browser.viewType}.jsp" />
</cpn:component>