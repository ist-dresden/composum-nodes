/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.browser.merged');

    (function (merged, browser, core) {

        merged.getTab = function () {
            return core.getView('.node-view-panel .merged', merged.Tab);
        };

        merged.Tab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$toolbar = this.$('.merged-toolbar');
                this.$content = this.$('.detail-content');
                this.$toolbar.find('.refresh').click(_.bind(this.reload, this));
            },

            initContent: function () {
                this.$el.find('.merged-list_resource').each(_.bind(function (index, el) {
                    var properties = core.getView(el, merged.Properties, {
                        search: false
                    });
                    properties.reload();
                }, this));
            },

            reload: function () {
                core.ajaxGet('/libs/composum/nodes/browser/components/merged.content.html'
                    + browser.getCurrentPath(), undefined,
                    _.bind(function (content) {
                        this.$content.html(content);
                        this.initContent();
                    }, this));
            }
        });

        merged.Properties = browser.PropertiesTab.extend({

            initialize: function (options) {
                browser.PropertiesTab.prototype.initialize.call(this, options);
            },

            getPath: function () {
                return this.$el.data('path');
            },

            propertyChanged: function (path) {
                this.reload();
            }
        });

    })(CPM.nodes.browser.merged, CPM.nodes.browser, CPM.core);

})();
