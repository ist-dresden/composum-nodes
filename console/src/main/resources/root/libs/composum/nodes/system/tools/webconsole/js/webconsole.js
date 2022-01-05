(function () {
    'use strict';
    CPM.namespace('nodes.tools.webconsole');

    (function (webconsole, core) {

        webconsole.settings = {
            url: {
                initial: '/system/console/bundles'
            }
        };

        webconsole.View = Backbone.View.extend({

            initialize: function (options) {
                this.$iframe = $('.content-wrapper iframe');
                this.$iframe.on('load.Webconsole', _.bind(this.onFrameLoad, this));
                var url = core.console.getProfile().get('tools-webconsole', 'url', webconsole.settings.url.initial);
                core.ajaxHead(url, {}, _.bind(function () {
                    this.$iframe.attr('src', core.getContextUrl(url));
                }, this), _.bind(function () {
                    this.$iframe.attr('src', core.getContextUrl(webconsole.settings.url.initial));
                }, this));
            },

            onFrameLoad: function (event) {
                var url = event.currentTarget.contentDocument.URL;
                url = url.replace(new RegExp('https?://[^/]+/'), '/');
                core.console.getProfile().set('tools-webconsole', 'url', url);
            }
        });

        webconsole.view = core.getView('#tools-webconsole', webconsole.View);

    })(CPM.nodes.tools.webconsole, CPM.core);
})();
