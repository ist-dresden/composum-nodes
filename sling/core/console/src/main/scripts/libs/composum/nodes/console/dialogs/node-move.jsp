<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div id="node-move-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog form-panel">
            <div class="modal-content">
                <form class="widget-form">

                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Move Node</h4>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>

                        <div class="form-group">
                            <label class="control-label">Node to move <span>(path)</span></label>
                            <div class="path input-group widget path-widget" data-rules="mandatory">
                                <input name="target-node" class="form-control" type="text"/>
                                <span class="input-group-btn">
                                    <button class="select btn btn-default" type="button" title="Select Repository Path">
                                        ...
                                    </button>
                                </span>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="control-label">Target Path <span>(new parent path)</span></label>
                            <div class="path input-group widget path-widget" data-rules="mandatory">
                                <input name="path" class="form-control" type="text"/>
                                <span class="input-group-btn">
                                    <button class="select btn btn-default" type="button" title="Select Repository Path">
                                        ...
                                    </button>
                                </span>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-xs-6">
                                <div class="form-group name">
                                    <label class="control-label">New Node Name</label>
                                    <input name="name" class="form-control widget text-field-widget" type="text"
                                           placeholder="enter node name" autofocus>
                                </div>
                            </div>
                            <div class="col-xs-4">
                                <div class="form-group before">
                                    <label class="control-label">Order Before &nbsp; &nbsp; &nbsp; or at ...</label>
                                    <input name="before" class="form-control widget text-field-widget" type="text"
                                           placeholder="node name" autofocus>
                                </div>
                            </div>
                            <div class="col-xs-2">
                                <div class="form-group index">
                                    <label class="control-label">Position</label>
                                    <div class="index input-group widget number-field-widget" data-options="-1">
                                        <input name="index" type="text" class="form-control"/>
                                        <span class="input-group-addon spinner"><span
                                                class="decrement fa fa-minus" title="decrement"></span><span
                                                class="increment fa fa-plus" title="increment"></span></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary move">Move</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</cpn:component>