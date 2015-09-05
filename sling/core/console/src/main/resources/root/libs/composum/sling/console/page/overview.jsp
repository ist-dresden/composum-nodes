<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<cpn:component id="status" type="com.composum.sling.core.console.Consoles">
<html>
<sling:call script="head.jsp"/>
<link rel="stylesheet" href="/libs/composum/sling/console/page/css/content.css" />
<body id="overview" class="console">
  <div id="ui">
    <sling:call script="dialogs.jsp"/>
    <sling:call script="navbar.jsp"/>
    <div class="content">
      <div class="row">
        <div class="left col-md-3 col-sm-4 col-xs-12">
          <img src="/libs/composum/sling/console/page/images/composum-nodes.png" alt="Composum Sling nodes logo" />
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
          <a href="/bin/browser.html"><h2>the Browser</h2></a>
        </div>
        <div class="main col-md-9 col-sm-8 col-xs-12">
          <section>
            <p>
              With the browser you can deep dive through your respository.
            </p>
          </section>
        </div>
      </div>
      <%--
      <div class="row aspect">
        <div class="left col-md-3 col-sm-4 col-xs-12">
          <img src="/libs/composum/sling/console/page/images/composum-pages.png" alt="Composum Sling nodes logo" />
        </div>
        <div class="main col-md-9 col-sm-8 col-xs-12">
          <h1>a simple CMS <small>(planned)</small></h1>
        </div>
      </div>
      --%>
    </div>
  </div>
<sling:call script="script.jsp"/>
</body>
</html>
</cpn:component>