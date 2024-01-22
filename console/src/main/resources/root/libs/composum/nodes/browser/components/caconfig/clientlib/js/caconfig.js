/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.browser.caconfig');

    (function (caconfig, browser, core) {

        caconfig.getTab = function () {
            return core.getView('.node-view-panel .caconfig', caconfig.CaconfigTab);
        };

        caconfig.CaconfigTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$toolbar = this.$('.caconfig-toolbar');
                this.$content = this.$('.detail-content');
                this.$content.on('click', '.create-configuration-resource', _.bind(this.createConfigurationResource, this));
                this.$content.on('click', '.create-configuration-collection-resource', _.bind(this.createConfigurationResource, this));
                this.$toolbar.find('.add').click(_.bind(this.createConfigurationResource, this));
                this.$toolbar.find('.remove').click(_.bind(this.deleteConfigurationResource, this));
            },

            createConfigurationResource: function (event) {
                var $button = $(event.currentTarget);
                var path = $button.data('path');
                var name = $button.data('nodename');
                var type = $button.data('type');

                var dialog = core.nodes.getCreateNodeDialog();
                dialog.show(_.bind(function () {
                    if (path) {
                        dialog.initParentPath(path);
                    }
                    if (name) {
                        dialog.initName(name);
                    }
                    if (type) {
                        dialog.initType(type);
                    }
                }, this));
            },

            deleteConfigurationResource: function (event) {
                var selectedConfiguration = this.$content.find('.selected-configuration:checked');
                var path = selectedConfiguration.data('path');
                var name = selectedConfiguration.data('name');
                if (!path || !name) {
                    return;
                }
                var dialog = core.nodes.getDeleteNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setPath(path + '/' + name);
                    dialog.setSmart(false);
                }, this));
            },

        });

    })(CPM.nodes.browser.caconfig, CPM.nodes.browser, CPM.core);

})();
