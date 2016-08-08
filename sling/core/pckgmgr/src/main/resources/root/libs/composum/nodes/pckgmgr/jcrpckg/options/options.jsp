<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <div class="detail-view options ${pckg.cssClasses}">
        <div class="display-toolbar detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="advanced fa fa-pencil btn btn-default" title="Edit advanced properties"><span
                        class="label">Advanced</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="dependencies fa fa-link btn btn-default" title="Edit dependencies"><span
                        class="label">Dependencies</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="replaces fa fa-paste btn btn-default" title="Edit replaces"><span
                        class="label">Replaces</span>
                </button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <button class="refresh fa fa-refresh btn btn-default" title="Refresh"><span
                        class="label">Refresh view</span>
                </button>
            </div>
        </div>
        <div class="options-detail">
            <sling:include replaceSelectors="detail"/>
        </div>
    </div>
</cpn:component>