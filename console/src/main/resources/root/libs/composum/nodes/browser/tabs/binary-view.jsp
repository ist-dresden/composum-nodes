<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <c:if test="${browser.renderable}">
        <iframe src="/bin/cpm/nodes/node.download.bin${cpn:path(browser.filePath)}" width="100%" height="100%"></iframe>
    </c:if>
    <c:if test="${!browser.renderable}">
        <a href="/bin/cpm/nodes/node.download.bin${cpn:path(browser.filePath)}"
           class="file-download"><span class="file-symbol fa fa-${browser.fileIcon}"></span></a>
    </c:if>
</cpn:component>