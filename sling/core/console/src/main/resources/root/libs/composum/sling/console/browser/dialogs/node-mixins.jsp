<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
  <div id="node-mixins-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog form-panel">
      <div class="modal-content">
        <form class="widget-form">

          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">Node Mixins</h4>
          </div>
          <div class="modal-body">
            <div class="messages">
                <div class="alert alert-info">comming soon</div>
            </div>
            <input name="path" type="hidden">

            <div class="mixins widget multi-form-widget form-group" data-name="mixins">
              <label class="control-label">Mixins</label>
              <div class="multi-form-content">
                <div class="multi-form-item">
                  <input class="jcr-mixinTypes widget mixin-type-widget form-control" type="text" name="value"/>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer buttons">
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary submit">Ok</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</cpn:component>
