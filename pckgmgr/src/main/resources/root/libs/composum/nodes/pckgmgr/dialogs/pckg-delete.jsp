<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
    <%--@elvariable id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean"--%>
    <div id="pckg-delete-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog form-panel">
            <div class="modal-content">
                <form class="widget-form">

                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Delete Package</h4>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>

                        <input name="_charset_" type="hidden" value="UTF-8"/>
                        <c:if test="${not empty pckgmgr.registries}">
                            <div class="form-group registry">
                                <label class="control-label" for="pckg-delete-registry">Registry</label>
                                <select name="registry" class="widget select-widget form-control" id="pckg-delete-registry">
                                    <option value="" selected></option>
                                    <c:forEach items="${pckgmgr.registries}" var="registry">
                                        <option value="${registry.key}">${cpn:text(registry.value)}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </c:if>
                        <div class="form-group">
                            <label class="control-label" for="pckg-delete-group">Group</label>
                            <input name="group" class="widget primary-type-widget form-control" type="text" id="pckg-delete-group"
                                   placeholder="enter group name (path)" autofocus/>
                        </div>
                        <div class="form-group">
                            <label class="control-label" for="pckg-delete-name">Package Name</label>
                            <input name="name" class="widget text-field-widget form-control" type="text" id="pckg-delete-name"
                                   placeholder="enter package name" data-rules="mandatory"/>
                        </div>
                        <div class="form-group">
                            <label class="control-label" for="pck-delete-version">Version</label>
                            <input name="version" class="widget text-field-widget form-control" type="text" id="pck-delete-version"
                                   placeholder="enter version key (number)" data-rules="mandatory"/>
                        </div>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-danger delete">Delete</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</cpn:component>
