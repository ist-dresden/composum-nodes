<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<cpn:component id="bean" type="com.composum.sling.core.pckgmgr.regpckg.view.GroupBean" scope="request">
    <%--@elvariable id="bean" type="com.composum.sling.core.pckgmgr.regpckg.view.GroupBean"--%>
    <div class="package-list">
        <c:forEach items="${bean.multiVersionPackagePaths}" var="pckgpath">
            <sling:include replaceSuffix="${pckgpath}" replaceSelectors="cleanupSelection"
                           resourceType="composum/nodes/pckgmgr/regpckg"/>
        </c:forEach>
    </div>
</cpn:component>
