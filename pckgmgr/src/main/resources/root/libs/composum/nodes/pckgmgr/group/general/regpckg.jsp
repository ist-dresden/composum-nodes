<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="bean" type="com.composum.sling.core.pckgmgr.regpckg.view.GroupBean" scope="request">
    <%--@elvariable id="bean" type="com.composum.sling.core.pckgmgr.regpckg.view.GroupBean"--%>
    <div class="detail-panel group ${pckg.cssClasses}">
        <div class="display-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="refresh fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="cleanup fa fa-recycle btn btn-default"
                        title="Cleanup obsolete package versions"><span class="label">Cleanup</span></button>
            </div>
            <sling:include resourceType="composum/nodes/pckgmgr" replaceSelectors="helpbutton"/>
        </div>

        <div class="group-detail">
            <c:forEach items="${bean.packagePaths}" var="packagepath">
                <sling:include replaceSuffix="${packagepath}" replaceSelectors="listitem"
                               resourceType="composum/nodes/pckgmgr/regpckg/general"/>
            </c:forEach>
        </div>
    </div>
</cpn:component>
