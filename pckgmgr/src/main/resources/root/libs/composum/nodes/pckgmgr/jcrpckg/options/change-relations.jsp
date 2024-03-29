<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
    <div id="pckg-relations-change-dialog" class="change relations dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">

                <cpn:form classes="widget-form" method="POST" action="">

                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Change...</h4>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>
                        <input name="_charset_" type="hidden" value="UTF-8"/>

                        <div class="relations form-group widget multi-form-widget">
                            <div class="multi-form-content">
                                <div class="multi-form-item">
                                    <input class="relation widget text-field-widget form-control"
                                           type="text" placeholder="group:name:version"/>
                                </div>
                            </div>
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