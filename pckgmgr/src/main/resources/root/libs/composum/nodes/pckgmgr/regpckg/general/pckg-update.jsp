<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<div id="pckg-update-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content form-panel default">

            <cpn:form classes="widget-form" action="">

                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                            aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Change Package Properties</h4>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <div class="alert"></div>
                    </div>
                    <input name="_charset_" type="hidden" value="UTF-8"/>
                    <input name="path" type="hidden">

                    <div class="form-group">
                        <label class="control-label">Group</label>
                        <input name="group" class="widget primary-type-widget form-control" type="text"
                               placeholder="enter group name (path)" autofocus data-rules="mandatory"/>
                    </div>
                    <div class="form-group">
                        <label class="control-label">Package Name</label>
                        <input name="name" class="widget text-field-widget form-control" type="text"
                               placeholder="enter package name" data-rules="mandatory"/>
                    </div>
                    <div class="form-group">
                        <label class="control-label">Version</label>
                        <input name="version" class="widget text-field-widget form-control" type="text"
                               placeholder="enter version key (number)" data-rules="mandatory"/>
                    </div>
                    <div class="form-group">
                        <label class="control-label">Description</label>
                        <textarea name="jcr:description" class="text-area form-control" rows="4"></textarea>
                    </div>
                    <div class="form-group">
                        <label class="control-label">Include Versions</label>
                        <input name="includeVersions" class="widget checkbox-widget form-control" type="checkbox"/>
                    </div>

                </div>

                <div class="modal-footer buttons">
                    <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary save">Save</button>
                </div>

            </cpn:form>
        </div>
    </div>
</div>