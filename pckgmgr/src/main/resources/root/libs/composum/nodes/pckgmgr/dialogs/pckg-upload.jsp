<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean" scope="request">
  <%--@elvariable id="pckgmgr" type="com.composum.sling.core.pckgmgr.jcrpckg.view.PackageManagerBean"--%>
  <div id="pckg-upload-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content form-panel default">

        <cpn:form classes="widget-form" enctype="multipart/form-data" action="/bin/cpm/package.upload.json">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">Upload Package</h4>
          </div>
          <div class="modal-body">
            <div class="messages">
              <div class="alert"></div>
            </div>
            <input name="_charset_" type="hidden" value="UTF-8" />

            <c:if test="${not empty pckgmgr.registries}">
              <div class="form-group registry pckg-regpckg-mode-only">
                <label class="control-label" for="pckg-upload-registry">Registry</label>
                <select name="registry" class="widget select-widget form-control pckg-regpckg-mode-mandatory" id="pckg-upload-registry" data-rules="mandatory">
                  <option value="" selected></option>
                  <c:forEach items="${pckgmgr.registries}" var="registry">
                    <option value="${registry.key}">${cpn:text(registry.value)}</option>
                  </c:forEach>
                </select>
              </div>
            </c:if>

            <div class="form-group">
              <label class="control-label" for="pckg-upload-file">Package File</label>
              <input name="file" class="widget file-upload-widget form-control" type="file" id="pckg-upload-file"
                     data-options="hidePreview"/>
            </div>
            <div class="form-group">
              <label class="control-label" for="pckg-upload-force">Force Upload</label>
              <input name="force" class="widget checkbox-widget form-control" type="checkbox" id="pckg-upload-force"/>
            </div>
          </div>

          <div class="modal-footer buttons">
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary upload">Upload</button>
          </div>
        </cpn:form>
      </div>
    </div>
  </div>
</cpn:component>
