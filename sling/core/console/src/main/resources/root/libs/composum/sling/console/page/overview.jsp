<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="status" type="com.composum.sling.core.console.Consoles">
<html data-context-path="${slingRequest.contextPath}">
<sling:call script="head.jsp"/>
<cpn:clientlib path="composum/sling/console/page/css/content.css"/>
<body id="overview" class="console">
  <div id="ui">
    <sling:call script="dialogs.jsp"/>
    <sling:include resourceType="composum/sling/console/components/navbar"/>
    <div class="content">
      <div class="row">
        <div class="left col-md-3 col-sm-4 col-xs-12">
          <img src="${cpn:url(slingRequest,'/libs/composum/sling/console/page/images/composum-nodes.png')}" alt="Composum Sling nodes logo" />
        </div>
        <div class="main col-md-9 col-sm-8 col-xs-12">
          <h1>a 'Swiss Knife' <small>for the resource repository</small></h1>
          <p>
            The <name>Composum Sling 'nodes'</name> bundle gives you a tool to explore the repository and manage
            the content at the resource (JCR) level.
          </p>
        </div>
      </div>
      <div class="row">
        <div class="left col-md-3 col-sm-4 col-xs-12">
          <cpn:link href="/bin/browser.html"><h2>the Browser</h2></cpn:link>
          <p>
            <ul class="list-group">
              <li class="list-group-item">repository tree with configurable filters</li>
              <li class="list-group-item">node creation / deletion / move / reorder</li>
              <li class="list-group-item">property manipulation</li>
              <li class="list-group-item">binary data upload / download</li>
              <li class="list-group-item">component rendering and asset view</li>
              <li class="list-group-item">template based queries with history</li>
              <li class="list-group-item">JSON view, download and upload</li>
              <li class="list-group-item">ACL view and manipulation</li>
            </ul>
          </p>
        </div>
        <div class="main col-md-9 col-sm-8 col-xs-12">
          <section>
            <p>
              With the browser you can deep dive through your respository.
            </p>
            <ul class="nav nav-tabs">
              <li class="active"><a data-toggle="pill" href="#browser-props">Repository Browser</a></li>
              <li><a data-toggle="pill" href="#browser-view">Render / Image View</a></li>
              <li><a data-toggle="pill" href="#browser-code">Code View and Editing</a></li>
            </ul>
            <div class="tab-content">
              <div id="browser-props" class="tab-pane fade active in">
                <img class="img-responsive" src="${cpn:url(slingRequest,'/libs/composum/sling/console/guide/browser/images/browser-props.png')}" alt="Repository Browser and Query View" />
              </div>
              <div id="browser-view" class="tab-pane fade">
                <img class="img-responsive" src="${cpn:url(slingRequest,'/libs/composum/sling/console/guide/browser/images/browser-view.png')}" alt="Repository Browser Render View" />
              </div>
              <div id="browser-code" class="tab-pane fade">
                <img class="img-responsive" src="${cpn:url(slingRequest,'/libs/composum/sling/console/guide/browser/images/browser-code.png')}" alt="Repository Browser Code View" />
              </div>
            </div>
          </section>
        </div>
      </div>
      <%--
      <div class="row aspect">
        <div class="left col-md-3 col-sm-4 col-xs-12">
          <img src="${cpn:url(slingRequest,'/libs/composum/sling/console/page/images/composum-pages.png')}" alt="Composum Sling nodes logo" />
        </div>
        <div class="main col-md-9 col-sm-8 col-xs-12">
          <h1>a simple CMS <small>(planned)</small></h1>
        </div>
      </div>
      --%>
    </div>
  </div>
<sling:call script="script.jsp"/>
<sling:include resourceType="composum/sling/console/components/tryLogin"/>
</body>
</html>
</cpn:component>