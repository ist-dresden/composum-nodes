<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<div id="approval-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content form-panel">
      <form>
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
              aria-hidden="true">&times;</span></button>
          <h4 class="modal-title"></h4>
        </div>
        <div class="modal-body">
          <div class="messages">
            <div class="alert"></div>
          </div>
          <div class="content">
          </div>
        </div>
        <div class="modal-footer buttons">
          <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
          <button type="submit" class="btn btn-primary approve">&nbsp;OK&nbsp;</button>
        </div>
      </form>
    </div>
  </div>
</div>
