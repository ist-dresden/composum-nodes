<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
  <div id="access-policy-entry-dialog" class="add access-entry dialog modal fade"
       tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content form-panel default">

        <form class="widget-form default" enctype="multipart/form-data"
              action="/bin/core/system.accessPolicy.json${browser.current.pathEncoded}">

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
                  <select name="privilege" class="widget combo-box-widget form-control">
                    <option value="jcr:read">jcr:read</option>
                    <option value="jcr:modifyProperties">jcr:modifyProperties</option>
                    <option value="jcr:addChildNodes">jcr:addChildNodes</option>
                    <option value="jcr:removeNode">jcr:removeNode</option>
                    <option value="jcr:removeChildNodes">jcr:removeChildNodes</option>
                    <option value="jcr:readAccessControl">jcr:readAccessControl</option>
                    <option value="jcr:modifyAccessControl">jcr:modifyAccessControl</option>
                    <option value="jcr:lockManagement">jcr:lockManagement</option>
                    <option value="jcr:versionManagement">jcr:versionManagement</option>
                    <option value="jcr:nodeTypeManagement">jcr:nodeTypeManagement</option>
                    <option value="jcr:retentionManagement">jcr:retentionManagement</option>
                    <option value="jcr:lifecycleManagement">jcr:lifecycleManagement</option>
                    <option value="jcr:write">jcr:write</option>
                    <option value="jcr:all">jcr:all</option>
                    <option value="jcr:workspaceManagement">jcr:workspaceManagement</option>
                    <option value="jcr:nodeTypeDefinitionManagement">jcr:nodeTypeDefinitionManagement</option>
                    <option value="jcr:namespaceManagement">jcr:namespaceManagement</option>
                    <option value="rep:write">rep:write</option>
                    <option value="rep:privilegeManagement">rep:privilegeManagement</option>
                    <option value="rep:userManagement">rep:userManagement</option>
                    <option value="rep:readNodes">rep:readNodes</option>
                    <option value="rep:readProperties">rep:readProperties</option>
                    <option value="rep:addProperties">rep:addProperties</option>
                    <option value="rep:alterProperties">rep:alterProperties</option>
                    <option value="rep:removeProperties">rep:removeProperties</option>
                    <option value="rep:indexDefinitionManagement">rep:indexDefinitionManagement</option>
                  </select>
                </div>
              </div>
            </div>
            <div class="restrictions widget multi-form-widget form-group" data-name="restrictions">
              <label class="control-label">Restrictions</label>
              <div class="multi-form-content">
                <div class="multi-form-item">
                  <select name="restrictionKey" class="key widget combo-box-widget form-control">
                    <option value="rep:glob">rep:glob</option>
                    <option value="rep:ntNames">rep:ntNames</option>
                    <option value="rep:prefixes">rep:prefixes</option>
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

        </form>
      </div>
    </div>
  </div>
</cpn:component>
