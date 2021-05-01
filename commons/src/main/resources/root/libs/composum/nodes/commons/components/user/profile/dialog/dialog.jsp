<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:bundle basename="composum-nodes"/>
<cpn:component var="profile" type="com.composum.sling.core.user.UserProfile" scope="request">
    <div id="user-create-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">
                <cpn:form classes="widget-form" enctype="multipart/form-data"
                          action="${profile.path}" method="POST">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">${cpn:i18n(slingRequest,'Edit User Profile')}</h4>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="panel panel-warning hidden">
                                <div class="panel-heading"></div>
                                <div class="panel-body hidden"></div>
                            </div>
                        </div>
                        <input name="_charset_" value="UTF-8" type="hidden"/>

                        <div class="row">
                            <div class="col-xs-3">
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Title')}</label>
                                    <input name="title" value="${cpn:value(profile.values.title)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                            </div>
                            <div class="col-xs-4">
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Given Name')}</label>
                                    <input name="givenName" value="${cpn:value(profile.values.givenName)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                            </div>
                            <div class="col-xs-5">
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Family Name')}</label>
                                    <input name="familyName" value="${cpn:value(profile.values.familyName)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-xs-7">
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Job Title')}</label>
                                    <input name="jobTitle" value="${cpn:value(profile.values.jobTitle)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                            </div>
                            <div class="col-xs-1">
                            </div>
                            <div class="col-xs-4">
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Gender')}</label>
                                    <select name="gender" data-value="${cpn:value(profile.values.gender)}"
                                            class="widget select-widget form-control"
                                            data-options=":${cpn:i18n(slingRequest,'none')},female:${cpn:i18n(slingRequest,'female')},male:${cpn:i18n(slingRequest,'male')}">
                                    </select>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-xs-5">
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'E-Mail')}</label>
                                    <input name="email" value="${cpn:value(profile.values.email)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Phone Number')}</label>
                                    <input name="phoneNumber" value="${cpn:value(profile.values.phoneNumber)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                            </div>
                            <div class="col-xs-7">
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Street')}</label>
                                    <input name="street" value="${cpn:value(profile.values.street)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                                <div class="row">
                                    <div class="col-xs-4">
                                        <div class="form-group">
                                            <label class="control-label">${cpn:i18n(slingRequest,'Postal Code')}</label>
                                            <input name="postalCode" value="${cpn:value(profile.values.postalCode)}"
                                                   type="text" class="widget text-field-widget form-control"/>
                                        </div>
                                    </div>
                                    <div class="col-xs-8">
                                        <div class="form-group">
                                            <label class="control-label">${cpn:i18n(slingRequest,'City')}</label>
                                            <input name="city" value="${cpn:value(profile.values.city)}"
                                                   type="text" class="widget text-field-widget form-control"/>
                                        </div>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="control-label">${cpn:i18n(slingRequest,'Country')}</label>
                                    <input name="country" value="${cpn:value(profile.values.country)}"
                                           type="text" class="widget text-field-widget form-control"/>
                                </div>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="control-label">${cpn:i18n(slingRequest,'About me')}</label>
                            <div class="composum-widgets-richtext richtext-widget widget form-control"
                                 data-style="height:120">
                                <textarea class="composum-widgets-richtext_value rich-editor"
                                          name="aboutMe">${cpn:value(profile.values.aboutMe)}</textarea>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel"
                                data-dismiss="modal">${cpn:i18n(slingRequest,'Cancel')}</button>
                        <button type="submit" class="btn btn-primary create">${cpn:i18n(slingRequest,'Save')}</button>
                    </div>

                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>
