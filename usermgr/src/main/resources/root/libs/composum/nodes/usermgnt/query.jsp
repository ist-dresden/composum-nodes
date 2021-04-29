<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<div class="query-panel">
    <div class="query-head">
        <div class="query-actions action-bar" role="toolbar">
            <div class="query-toolbar btn-toolbar" role="toolbar">
                <div class="query-mode btn-group btn-group-sm" role="group">
                    <button type="button" data-mode="users" class="users fa fa-user-o btn btn-default"
                            title="${cpn:i18n(slingRequest,'Search Groups and Users')}"><span
                            class="label">${cpn:i18n(slingRequest,'Users')}</span>
                    </button>
                    <button type="button" data-mode="paths" class="paths fa fa-folder-o btn btn-default"
                            title="${cpn:i18n(slingRequest,'Search Affected Paths')}"><span
                            class="label">${cpn:i18n(slingRequest,'Paths')}</span>
                    </button>
                    <button type="button" data-mode="graph" class="graph fa fa-map-o btn btn-default"
                            title="${cpn:i18n(slingRequest,'Search using the Graph')}"><span
                            class="label">${cpn:i18n(slingRequest,'Graph')}</span>
                    </button>
                </div>
                <cpn:form role="search" action="/bin/cpm/usermanagement.query.json" method="GET"
                          class="query-form">
                    <div class="query-input btn-group btn-group-sm" role="group">
                        <select name="type" class="type select form-control">
                            <option value="">${cpn:i18n(slingRequest,'All')}</option>
                            <option value="user">${cpn:i18n(slingRequest,'User')}</option>
                            <option value="group">${cpn:i18n(slingRequest,'Group')}</option>
                            <option value="service">${cpn:i18n(slingRequest,'Service')}</option>
                        </select>
                        <input name="name" type="text" class="name pattern form-control"
                               placeholder="${cpn:i18n(slingRequest,'Authorizable Name')}">
                        <input name="path" type="text" class="path pattern form-control"
                               placeholder="${cpn:i18n(slingRequest,'Authorizable Path')}">
                        <input name="text" type="text" class="text hidden pattern form-control"
                               placeholder="${cpn:i18n(slingRequest,'Affected Path')}">
                    </div>
                    <div class="btn-group btn-group-sm" role="group">
                        <button type="submit" class="exec fa fa-play btn btn-default"
                                title="${cpn:i18n(slingRequest,'Execute query')}..."></button>
                    </div>
                </cpn:form>
            </div>
        </div>
    </div>
    <div class="query-result">
        <table class="table table-striped table-hover table-condensed">
            <tbody>
            </tbody>
        </table>
        <div class="query-canvas hidden">
            <div class="composum-nodes-usermgr-paths_wrapper">
                <div class="composum-nodes-usermgr-paths"></div>
            </div>
        </div>
    </div>
</div>
