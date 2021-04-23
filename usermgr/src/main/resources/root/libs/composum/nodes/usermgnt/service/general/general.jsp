<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.ServiceUser" scope="request">
    <div class="service-user detail-panel">
        <div class="detail-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="reload fa fa-refresh btn btn-default"
                        title="${cpn:i18n(slingRequest,'Reload')}"><span
                        class="label">${cpn:i18n(slingRequest,'Reload')}</span>
                </button>
            </div>
        </div>
        <div class="service-user-container">
            <div class="authorizable">
                <div class="top-panel">
                    <div class="headline">
                        <div class="status">
                            <div class="label">${cpn:i18n(slingRequest,'Service User')}</div>
                            <div class="indicator system fa fa-toggle-on">
                                &nbsp;
                            </div>
                        </div>
                        <div class="title" title="${model.path}">
                            <cpn:text tagName="span" class="name">${model.service.serviceName}</cpn:text>
                            <cpn:text test="${not empty model.service.serviceInfo}"
                                      tagName="span" class="info">: ${model.service.serviceInfo}</cpn:text>
                        </div>
                    </div>
                </div>
                <div class="info-panel">
                    <cpn:div class="groups">
                        <sling:call script="groups.jsp"/>
                    </cpn:div>
                </div>
            </div>
        </div>
    </div>
</cpn:component>