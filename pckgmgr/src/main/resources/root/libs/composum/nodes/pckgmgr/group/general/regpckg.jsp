<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="bean" type="com.composum.sling.core.pckgmgr.regpckg.view.GroupBean" scope="request">
    <div class="detail-panel group">
        <div class="group-detail">
            <c:forEach items="${bean.packagePaths}" var="packagepath">
                <sling:include replaceSuffix="${packagepath}" replaceSelectors="listitem"
                               resourceType="composum/nodes/pckgmgr/regpckg/general"/>
            </c:forEach>
        </div>
    </div>
</cpn:component>
