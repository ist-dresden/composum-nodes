<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div id="file-update-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content form-panel default">

            <cpn:form classes="widget-form" enctype="multipart/form-data"
                      action="/bin/cpm/nodes/node.fileUpdate.json" method="POST">

                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                            aria-hidden="true">&times;</span></button>
                    <cpn:text tagName="h4" class="modal-title" i18n="true">Change File Content</cpn:text>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <div class="alert"></div>
                    </div>

                    <input name="_charset_" type="hidden" value="UTF-8"/>
                    <input name="path" type="hidden"/>
                    <div class="form-group binary">
                        <cpn:text tagName="label" class="control-label" i18n="true">Select File</cpn:text>
                        <input name="file" class="widget file-upload-widget form-control" type="file"/>
                    </div>
                    <div class="form-group binary">
                        <div class="checkbox">
                            <label><input name="adjustLastModified" class="smart" type="checkbox"
                                          value="">${cpn:value(cpn:i18n(slingRequest,"adjust 'jcr:lastModified' properties"))}</label>
                        </div>
                    </div>
                </div>

                <div class="modal-footer buttons">
                    <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary update">Update</button>
                </div>
            </cpn:form>
        </div>
    </div>
</div>
