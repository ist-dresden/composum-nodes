<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<cpn:defineObjects/>
<cpn:component id="model" type="com.composum.sling.nodes.components.CAConfigModel" scope="request">
    <%--@elvariable id="model" type="com.composum.sling.nodes.components.CAConfigModel"--%>
    <div id="browser-view-property-dialog"
         class="change property dialog modal fade composum-nodes-caconfig-propertyedit"
         tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">

                <cpn:form classes="widget-form default" enctype="multipart/form-data"
                          action="/bin/cpm/nodes/property.bin">

                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Create or Change CA-Property</h4>
                    </div>

                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                            <p class="description"></p>
                            <p>
                                <div class="propertiesContainer hidden">
                                    <label class="control-label">Additional properties:</label>
                                    <div class="propertiesContent"></div>
                                </div>
                            </p>
                        </div>

                        <div class="form-group path">
                            <label class="control-label">Path</label>
                            <input name="path" class="widget property-name-widget form-control" type="text"
                                   value="${model.path}"
                                   data-rules="mandatory" readonly>
                        </div>

                        <div class="row">
                            <div class="col-lg-10 col-md-10 col-sm-10 col-xs-10">
                                <div class="form-group name">
                                    <label class="control-label">Name</label>
                                    <input name="name" class="widget property-name-widget form-control" type="text"
                                           data-rules="mandatory" readonly>
                                </div>
                            </div>
                            <div class="col-lg-2 col-md-2 col-sm-2 col-xs-2">
                                <div class="form-group required">
                                    <label class="control-label">Required</label>
                                    <input name="required" class="multi-select-box form-control widget checkbox-widget"
                                           type="checkbox" onclick="return false;" disabled>
                                </div>
                            </div>
                        </div>

                        <div class="row">
                            <div class="col-lg-7 col-md-7 col-sm-7 col-xs-7">
                                <div class="form-group type">
                                    <label class="control-label">Type</label>
                                    <input name="type" class="widget property-name-widget form-control" type="text"
                                           data-rules="mandatory" readonly>
                                </div>
                            </div>
                            <div class="col-lg-3 col-md-3 col-sm-3 col-xs-3">
                                <div class="form-group subtype">
                                    <label class="control-label">Subtype</label>
                                    <select name="subtype" class="subtype-select widget select-widget form-control">
                                        <option value="string">string value</option>
                                        <option value="plaintext">plain text</option>
                                        <option value="richtext">rich text</option>
                                    </select>
                                </div>
                            </div>
                            <div class="col-lg-2 col-md-2 col-sm-2 col-xs-2">
                                <div class="form-group multi">
                                    <label class="control-label">Multi</label>
                                    <input name="multicheckbox"
                                           class="multi-select-box form-control widget checkbox-widget"
                                           type="checkbox" onclick="return false;" disabled>
                                    <input name="multi" type="hidden">
                                </div>
                            </div>
                        </div>
                        <sling:call script="${composumBase}composum/nodes/browser/dialogs/property-value.jsp"/>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-danger delete">Delete</button>
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-primary default save">Save</button>
                        <button type="button" class="btn btn-primary binary upload">Upload</button>
                    </div>

                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>
