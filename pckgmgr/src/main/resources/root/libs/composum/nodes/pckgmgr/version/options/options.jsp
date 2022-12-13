<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.regpckg.view.VersionBean" scope="request">
    <div class="detail-view options ${pckg.cssClasses}">
        <div class="display-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="refresh fa fa-refresh btn btn-default" title="Refresh"><span
                        class="label">Refresh view</span>
                </button>
            </div>
        </div>
        <div class="options-detail">
            <sling:include replaceSelectors="detail" />
        </div>
    </div>
</cpn:component>
