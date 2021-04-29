<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="group" type="com.composum.sling.core.usermanagement.view.Group" scope="request">
    <div class="group detail-panel">
        <div class="detail-tabs action-bar btn-toolbar" role="toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <a class="general fa fa-address-card-o btn btn-default" href="#general" data-group="general"
                   title="${cpn:i18n(slingRequest,'General')}"><span
                        class="label">${cpn:i18n(slingRequest,'General')}</span></a>
                <a class="groups fa fa-users text-muted btn btn-default" href="#groups" data-group="groups"
                   title="${cpn:i18n(slingRequest,'Groups')}"><span
                        class="label">${cpn:i18n(slingRequest,'Groups')}</span></a>
                <a class="members fa fa-user btn btn-default" href="#members" data-group="members"
                   title="${cpn:i18n(slingRequest,'Members')}"><span
                        class="label">${cpn:i18n(slingRequest,'Members')}Members</span></a>
                <a class="paths fa fa-folder-o btn btn-default" href="#paths" data-group="paths"
                   title="${cpn:i18n(slingRequest,'Affected Paths')}"><span
                        class="label">${cpn:i18n(slingRequest,'Affected Paths')}</span></a>
                <a class="graph fa fa-map-o btn btn-default" href="#graph" data-group="view"
                   title="${cpn:i18n(slingRequest,'Graph')}"><span
                        class="label">${cpn:i18n(slingRequest,'Graph')}</span></a>
            </div>
        </div>
        <div class="detail-content">
        </div>
    </div>
</cpn:component>
