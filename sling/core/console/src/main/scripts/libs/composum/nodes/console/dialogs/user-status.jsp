<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="status" type="com.composum.sling.nodes.console.Consoles">
  <div id="user-status-dialog" class="dialog modal fade" role="dialog" aria-hidden="true">
    <div class="modal-dialog form-panel">
      <cpn:form charset="UTF-8" action="/j_security_check" method="post">

        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                aria-hidden="true">&times;</span></button>
            <h4 class="current-user modal-title">Current User: ${status.currentUser}</h4>
            <h4 class="needs-authorization modal-title">Authorization needed!</h4>
          </div>

          <div class="modal-body">
            <div class="alert alert-hidden" role="alert"></div>
              <input type="hidden" name="_charset_" value="UTF-8" />
              <input type="hidden" name="j_validate" value="true"/>

              <div class="form-group">
                <label for="j_username" class="control-label">Username</label>
                <input id="j_username" name="j_username" type="text" accesskey="u" class="form-control" autofocus>
              </div>
              <div class="form-group">
                <label for="j_password" class="control-label">Password</label>
                <input id="j_password" name="j_password" type="password" accesskey="p" class="form-control">
              </div>
          </div>

          <div class="modal-footer buttons">
            <button type="button" class="btn btn-warning logout" data-url="${status.logoutUrl}">Logout</button>
            <button type="button" class="btn btn-default cancel" data-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary login">Login</button>
          </div>
        </div>

      </cpn:form>
    </div>
  </div>
</cpn:component>