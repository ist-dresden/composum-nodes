<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
    <ol class="breadcrumbs">
        <c:forEach var="parent" items="${browser.parents}">
            <li><a href="${slingRequest.contextPath}/bin/browser.html${parent.pathEncoded}">${parent.nameEscaped}</a></li>
        </c:forEach>
        <li class="active"><a href="/bin/browser.html${browser.current.pathEncoded}">${browser.current.nameEscaped}</a></li>
    </ol>
</cpn:component>
