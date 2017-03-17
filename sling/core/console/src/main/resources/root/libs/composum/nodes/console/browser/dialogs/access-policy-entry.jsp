<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div id="access-policy-entry-dialog" class="add access-entry dialog modal fade"
       tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content form-panel default">

        <cpn:form classes="widget-form default" enctype="multipart/form-data"
                  action="/bin/cpm/core/system.accessPolicy.json${browser.current.pathEncoded}">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">Add Access Policy Entry</h4>
          </div>

          <div class="modal-body">
            <div class="messages">
              <div class="alert"></div>
            </div>
            <input name="path" type="hidden">
            <div class="form-group principal">
              <label class="control-label">Principal</label>
              <input name="principal" class="widget text-widget form-control" type="text">
            </div>
            <div class="form-group rule">
              <div class="widget radio-group-widget form-control">
                <div class="radio-inline allow"><label><input type="radio" name="rule" value="allow">Allow</label></div>
                <div class="radio-inline deny"><label><input type="radio" name="rule" value="deny">Deny</label></div>
              </div>
            </div>
            <div class="privileges widget multi-form-widget form-group" data-name="privileges">
              <label class="control-label">Privileges</label>
              <div class="multi-form-content">
                <div class="multi-form-item">
                  <select name="privilege" class="widget select-widget form-control">
                  </select>
                </div>
              </div>
            </div>
            <div class="restrictions widget multi-form-widget form-group" data-name="restrictions">
              <label class="control-label">Restrictions</label>
              <div class="multi-form-content">
                <div class="multi-form-item">
                  <select name="restrictionKey" class="key widget select-widget form-control">
                    <%--<option value="rep:glob">rep:glob</option>--%>
                    <%--<option value="rep:ntNames">rep:ntNames</option>--%>
                    <%--<option value="rep:prefixes">rep:prefixes</option>--%>
                  </select>
                  <input name="restrictionValue" class="value widget text-widget form-control" type="text">
                </div>
              </div>
            </div>
          </div>

          <div class="modal-footer buttons">
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-primary save">Save</button>
          </div>

        </cpn:form>
      </div>
    </div>
  </div>
</cpn:component>
