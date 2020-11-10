<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.view.PackageManagerBean" scope="request">
    <div id="pckg-filter-dialog" class="change filter dialog modal fade" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content form-panel default">

                <cpn:form classes="widget-form" action="">

                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">Create or Change Package Filter</h4>
                    </div>
                    <div class="modal-body">
                        <div class="messages">
                            <div class="alert"></div>
                        </div>
                        <input name="_charset_" type="hidden" value="UTF-8"/>
                        <input name="index" type="hidden" value="-1"/>

                        <div class="form-group">
                            <label class="control-label">Root Path</label>
                            <div class="root widget path-widget input-group" data-rules="mandatory">
                                <input name="root" type="text" class="form-control" placeholder="Select path..."
                                       autofocus>
                                <span class="input-group-btn">
                                  <button class="select btn btn-default" type="button" title="Select Repository Path">
                                      ...
                                  </button>
                                </span>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="control-label">Import Mode</label>
                            <select name="importMode" class="import-mode widget select-widget form-control">
                                <option value="REPLACE" selected="true">replace (default)</option>
                                <option value="MERGE">merge (existing content is not modified)</option>
                                <option value="UPDATE">update (existing content is not deleted)</option>
                            </select>
                        </div>
                        <div class="rules form-group widget filter-rules-widget">
                            <label class="control-label">Filter Set</label>
                            <div class="multi-form-content">
                                <div class="multi-form-item">
                                    <select name="ruleType" class="type widget select-widget form-control">
                                        <option value="include" selected="true">include</option>
                                        <option value="exclude">exclude</option>
                                    </select>
                                    <input name="ruleExpression" class="pattern widget text-field-widget form-control"
                                           type="text" placeholder="regular expression"/>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer buttons">
                        <button type="button" class="btn btn-danger delete">Delete</button>
                        <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary create">Create</button>
                        <button type="submit" class="btn btn-primary save">Save</button>
                    </div>
                </cpn:form>
            </div>
        </div>
    </div>
</cpn:component>