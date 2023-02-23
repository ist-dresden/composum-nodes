<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
    <%--@elvariable id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean"--%>
    <div id="pckg-cleanup-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog form-panel">
            <div class="modal-content">
                <cpn:form classes="widget-form" action="/bin/cpm/package.cleanupObsoleteVersions.json">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <cpn:text tagName="h4" class="modal-title text"
                                  i18n="true">Cleanup Obsolete Package Versions</cpn:text>
                        <cpn:text class="text" i18n="true">Caution: this cannot be undone.</cpn:text>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="loading-message"><cpn:text class="text"
                                                                   i18n="true">Checking for obsolete versions...</cpn:text></div>
                            <div class="messages">
                                <div class="hidden">
                                    <div class="panel-heading"></div>
                                    <div class="panel-body"></div>
                                </div>
                            </div>
                        </div>
                        <input name="_charset_" type="hidden" value="UTF-8"/>

                        <div class="form-group">
                            <label class="control-label">${cpn:i18n(slingRequest,'Path')}</label>
                            <input name="path" type="text" readonly="readonly"
                                   class="widget text-field-widget form-control"/>
                        </div>

                        <div class="versioncheckboxes">
                        </div>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-danger cleanup">Cleanup</button>
                    </div>
                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>
