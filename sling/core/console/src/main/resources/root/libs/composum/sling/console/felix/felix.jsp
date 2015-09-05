<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects/>
<html>
<head>
  <sling:call script="/libs/composum/sling/console/page/head.jsp"/>
  <style>
    html, body, #ui { width: 100%; height: 100%; }
    body { position: relative; }
    #content-wrapper {
      position: absolute;
      top: 48px;
      left: -4%;
      right: -4%;
      bottom: 7px;
      overflow-y: scroll;
      overflow-x: hidden;
    }
  </style>
</head>
<body id="felix" class="console">
  <div id="ui">
    <sling:call script="/libs/composum/sling/console/page/dialogs.jsp"/>
    <sling:call script="/libs/composum/sling/console/page/navbar.jsp"/>
    <div id="content-wrapper">
      <iframe width="100%" height="100%" src=""></iframe>
    </div>
  </div>
  <sling:call script="/libs/composum/sling/console/page/script.jsp"/>
  <script>
    (function(core) {
        'use strict';
        core.felix = core.felix || {};

    (function(felix) {

        felix.View = Backbone.View.extend({

            initialize: function(options) {
                this.$iframe=$('#content-wrapper iframe');
                this.$iframe.load(_.bind (this.onFrameLoad, this));
                var url = core.console.getProfile().get('felix','url','/system/console');
                this.$iframe.attr('src', url);
            },

            onFrameLoad: function(event) {
                var url = event.currentTarget.contentDocument.URL;
                url = url.replace(new RegExp('https?://[^/]+/'), '/');
                core.console.getProfile().set('felix','url',url);
            }
        });

        felix.view = core.getView('#felix', felix.View);

    })(core.felix);
    })(window.core);
  </script>
</body>
</html>