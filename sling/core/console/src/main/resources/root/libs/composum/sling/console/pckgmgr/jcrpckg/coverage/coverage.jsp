<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.PackageBean" scope="request">
    <div class="detail-panel coverage ${pckg.cssClasses} full-table-view">
        <div class="table-toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button class="reload fa fa-refresh btn btn-default" title="Reload"><span class="label">Reload</span></button>
            </div>
        </div>
        <div class="table-container">
            <table class="coverage-table"
                   data-path="/bin/core/package.coverage.json${pckg.path}"
                   data-toolbar=".detail-content .detail-panel.coverage .table-toolbar">
            </table>
        </div>
    </div>
</cpn:component>