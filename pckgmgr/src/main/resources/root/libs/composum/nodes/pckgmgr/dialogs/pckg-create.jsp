<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
    <%--@elvariable id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean"--%>
    <div id="pckg-create-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">

                <cpn:form classes="widget-form" action="/bin/cpm/package.create.json">

                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Create New Package</h4>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>
                        <input name="_charset_" type="hidden" value="UTF-8"/>

                        <div class="form-group">
                            <label class="control-label" for="pckg-create-group">Group</label>
                            <input name="group" class="widget primary-type-widget form-control" type="text" id="pckg-create-group"
                                   placeholder="enter group name (path)"/>
                        </div>
                        <div class="form-group">
                            <label class="control-label" for="pckg-create-name">Package Name</label>
                            <input name="name" class="widget text-field-widget form-control" type="text" id="pckg-create-name"
                                   placeholder="enter package name" autofocus data-rules="mandatory"/>
                        </div>
                        <div class="form-group">
                            <label class="control-label" for="pckg-create-version">Version</label>
                            <input name="version" class="widget text-field-widget form-control" type="text" id="pckg-create-version"
                                   placeholder="enter version key (number)"/>
                        </div>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary create">Create</button>
                    </div>
                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>
