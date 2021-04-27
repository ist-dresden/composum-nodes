<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="model" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="user detail-panel">
        <div class="detail-tabs action-bar btn-toolbar" role="toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <a class="general fa fa-address-card-o btn btn-default" href="#general" data-group="general" title="General"><span
                        class="label">General</span></a>
                <a class="profile fa fa-list btn btn-default" href="#profile" data-group="profile"
                   title="Profile"><span class="label">Profile</span></a>
                <a class="preferences fa fa-wrench btn btn-default" href="#preferences" data-group="preferences"
                   title="Preferences"><span class="label">Preferences</span></a>
                <a class="groups fa fa-users text-muted btn btn-default" href="#groups" data-group="groups"
                   title="Groups"><span class="label">Groups</span></a>
                <a class="paths fa fa-folder-o btn btn-default" href="#paths" data-group="paths" title="Affected Paths"><span
                        class="label">Affected Paths</span></a>
                <a class="graph fa fa-map-o btn btn-default" href="#graph" data-group="view" title="Graph"><span
                        class="label">Graph</span></a>
            </div>
        </div>
        <div class="detail-content">
        </div>
    </div>
</cpn:component>
