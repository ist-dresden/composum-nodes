<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<html data-context-path="${slingRequest.contextPath}">
<head>
    <sling:call script="/libs/composum/nodes/console/page/head.jsp"/>
    <style>
        html, body, #ui {
            width: 100%;
            height: 100%;
        }

        body {
            position: relative;
        }

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
    <sling:include resourceType="/libs/composum/nodes/console/dialogs" replaceSelectors="minimal"/>
    <sling:include resourceType="composum/nodes/console/components/navbar"/>
    <div id="content-wrapper">
        <iframe width="100%" height="100%" src=""></iframe>
    </div>
</div>
<sling:call script="/libs/composum/nodes/console/page/script.jsp"/>
<script>
    (function (core) {
        'use strict';
        core.felix = core.felix || {};

        (function (felix) {

            felix.settings = {
                url: {
                    initial: '/system/console/bundles'
                }
            };

            felix.View = Backbone.View.extend({

                initialize: function (options) {
                    this.$iframe = $('#content-wrapper iframe');
                    this.$iframe.on('load.EmbeddedFelix', _.bind(this.onFrameLoad, this));
                    var url = core.console.getProfile().get('felix', 'url', felix.settings.url.initial);
                    core.ajaxHead(url, {}, _.bind(function () {
                        this.$iframe.attr('src', core.getContextUrl(url));
                    }, this), _.bind(function () {
                        this.$iframe.attr('src', core.getContextUrl(felix.settings.url.initial));
                    }, this));
                },

                onFrameLoad: function (event) {
                    var url = event.currentTarget.contentDocument.URL;
                    url = url.replace(new RegExp('https?://[^/]+/'), '/');
                    core.console.getProfile().set('felix', 'url', url);
                }
            });

            felix.view = core.getView('#felix', felix.View);

        })(core.felix);
    })(window.core);
</script>
<sling:include resourceType="composum/nodes/console/components/tryLogin"/>
</body>
</html>