<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<div class="query-panel">
  <div class="query-head">
    <div class="query-actions action-bar btn-toolbar" role="toolbar">
      <cpn:form role="search" action="/bin/core/node.query.html" method="GET">
        <div class="input-group">
          <span class="select dropdown-toggle fa fa-history input-group-addon btn btn-default" data-toggle="dropdown" title="More actions..."></span>
          <ul class="select dropdown-menu" role="menu">
          </ul>
          <input type="text" class="form-control" placeholder="Search">
          <span class="exec glyphicon-triangle-right glyphicon input-group-addon btn btn-default" title="Execute query..."></span>
          <span class="filter fa fa-filter input-group-addon btn btn-default" title="Tree Filter ON/OFF"></span>
        </div>
      </cpn:form>
    </div>
    <div class="param-input-line">
      <div class="input-group template">
        <span class="key input-group-addon"></span>
        <input class="value form-control" type="text" />
      </div>
      <div class="form-inline">
      </div>
    </div>
  </div>
  <div class="query-result">
    <table class="table table-striped table-hover table-condensed">
      <tbody>
      </tbody>
    </table>
  </div>
</div>
</body>
</html>
