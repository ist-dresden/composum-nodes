<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="user" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <div id="user-changepw-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">
                <cpn:form classes="widget-form" enctype="multipart/form-data"
                          action="/bin/cpm/usermanagement.disable.json">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Change Password</h4>
                    </div>

                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>

                        <input name="_charset_" type="hidden" value="UTF-8" />
                        <div class="form-group">
                            <label class="control-label">Username</label>
                            <input name="username" class="widget text-field-widget form-control" type="text"
                                   data-rules="mandatory" readonly />
                        </div>
                        <c:if test="${!user.currentUserAdmin}">
	                        <div class="form-group">
	                            <label class="control-label">Old Password</label>
	                            <input name="oldPassword" class="widget text-field-widget form-control" type="password"
	                                   placeholder="enter password" data-rules="mandatory" />
	                        </div>
                        </c:if>                        
                        <div class="form-group">
                            <label class="control-label">Password</label>
                            <input name="password" class="widget text-field-widget form-control" type="password"
                                   placeholder="enter password" data-rules="mandatory" />
                        </div>

                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary create">Change</button>
                    </div>

                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>
