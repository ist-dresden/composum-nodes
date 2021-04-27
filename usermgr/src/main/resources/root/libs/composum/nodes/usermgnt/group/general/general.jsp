<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.Group" scope="request">
    <div class="general-group detail-tab">
        <div class="detail-toolbar">
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
                            <div class="label">${cpn:i18n(slingRequest,'Group')}</div>
                            <div class="indicator system fa fa-toggle-on">
                                &nbsp;
                            </div>
                        </div>
                        <div class="title" title="${model.path}">
                            <cpn:text tagName="span" class="id">${model.id}</cpn:text>
                        </div>
                    </div>
                </div>
                <div class="info-panel">
                    <div class="row">
                        <div class="col col-md-6 col-xs-12">
                            <div class="members">
                                <sling:call script="members.jsp"/>
                            </div>
                        </div>
                        <div class="col col-md-6 col-xs-12">
                            <div class="groups">
                                <sling:call script="groups.jsp"/>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</cpn:component>