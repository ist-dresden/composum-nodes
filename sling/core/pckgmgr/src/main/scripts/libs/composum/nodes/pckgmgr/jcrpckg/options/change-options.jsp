<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.view.PackageManagerBean" scope="request">
    <div id="pckg-options-change-dialog" class="change options dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">

                <cpn:form classes="widget-form" method="POST" action="">

                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Change Package Options</h4>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>
                        <input name="_charset_" type="hidden" value="UTF-8"/>

                        <div class="form-group">
                            <label class="control-label">AC Handling</label>
                            <select name="acHandling" class="ac-handling widget select-widget form-control">
                                <option value="" selected="true">-- --</option>
                                <option value="IGNORE">ignore</option>
                                <option value="OVERWRITE">overwrite</option>
                                <option value="MERGE">merge</option>
                                <option value="MERGE_PRESERVE">merge preserve</option>
                                <option value="CLEAR">clear</option>
                            </select>
                        </div>
                        <div class="form-group left">
                            <label class="control-label">Requires Root</label>
                            <input name="requiresRoot" class="requires-root widget checkbox-widget form-control" type="checkbox"/>
                        </div>
                        <div class="form-group right">
                            <label class="control-label">Requires Restart</label>
                            <input name="requiresRestart" class="requires-restart widget checkbox-widget form-control" type="checkbox"/>
                        </div>
                        <div class="form-group">
                            <label class="control-label">Provider Name</label>
                            <input name="providerName" class="provider-name widget text-field-widget form-control" type="text"/>
                        </div>
                        <div class="form-group">
                            <label class="control-label">Provider URL</label>
                            <input name="providerUrl" class="provider-url widget text-field-widget form-control" type="text"/>
                        </div>
                        <div class="form-group">
                            <label class="control-label">Provider Link</label>
                            <input name="providerLink" class="provider-link widget text-field-widget form-control" type="text"/>
                        </div>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary save">Update</button>
                    </div>
                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>