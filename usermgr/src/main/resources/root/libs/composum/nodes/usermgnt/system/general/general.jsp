<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div class="general-system detail-tab">
        <div class="detail-toolbar">
            <cpn:div test="${model.currentUserAdmin}" class="btn-group btn-group-sm" role="group">
                <button class="toggle-disabled fa fa-ban btn btn-default"
                        data-title-disable="${cpn:i18n(slingRequest,'Disable User')}"
                        data-title-enable="${cpn:i18n(slingRequest,'Enable User')}"><span
                        class="label"></span></button>
            </cpn:div>
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default"
                        title="${cpn:i18n(slingRequest,'Reload')}"><span
                        class="label">${cpn:i18n(slingRequest,'Reload')}</span>
                </button>
            </div>
        </div>
        <div class="detail-container">
            <div class="authorizable">
                <div class="top-panel">
                    <div class="headline">
                        <div class="status">
                            <div class="label">${cpn:i18n(slingRequest,model.userLabel)}</div>
                            <div class="indicator ${model.disabled?'disabled':'enabled'} fa fa-toggle-${model.disabled?'off':'on'}">
                                &nbsp;
                            </div>
                        </div>
                        <div class="title" title="${model.path}">
                            <cpn:text tagName="span" class="id">${model.id}</cpn:text>
                        </div>
                    </div>
                    <cpn:text test="${model.disabled}" class="disabled-reason">${model.disabledReason}</cpn:text>
                </div>
                <div class="info-panel">
                    <cpn:div class="row">
                        <cpn:div class="col col-md-6 col-xs-12">
                            <cpn:div class="members">
                                <sling:call script="members.jsp"/>
                            </cpn:div>
                        </cpn:div>
                        <cpn:div class="col col-md-6 col-xs-12">
                            <cpn:div class="groups">
                                <sling:call script="groups.jsp"/>
                            </cpn:div>
                        </cpn:div>
                    </cpn:div>
                </div>
            </div>
        </div>
    </div>
</cpn:component>