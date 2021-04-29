<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div id="user-create-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">
                <cpn:form classes="widget-form" enctype="multipart/form-data"
                          action="/bin/cpm/usermanagement.user.json">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                                aria-label="${cpn:i18n(slingRequest,'Close')}"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">${cpn:i18n(slingRequest,'Create New User')}</h4>
                    </div>

                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>

                        <input name="_charset_" type="hidden" value="UTF-8"/>
                        <div class="form-group">
                            <label class="control-label">${cpn:i18n(slingRequest,'Username')}</label>
                            <input name="username" class="widget text-field-widget form-control" type="text"
                                   placeholder="enter username" data-rules="mandatory" autocomplete="off"/>
                        </div>
                        <div class="form-group">
                            <label class="control-label">${cpn:i18n(slingRequest,'Password')}</label>
                            <input name="password" class="widget text-field-widget form-control" type="password"
                                   placeholder="enter password" data-rules="mandatory" autocomplete="off"/>
                        </div>

                        <div class="form-group">
                            <label class="control-label">${cpn:i18n(slingRequest,'Intermediate Path')}</label>
                            <input name="intermediatePath" class="widget text-field-widget form-control" type="text"
                                   placeholder="enter path"/>
                        </div>

                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel"
                                data-dismiss="modal">${cpn:i18n(slingRequest,'Cancel')}</button>
                        <button type="submit" class="btn btn-primary create">${cpn:i18n(slingRequest,'Create')}</button>
                    </div>

                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>
