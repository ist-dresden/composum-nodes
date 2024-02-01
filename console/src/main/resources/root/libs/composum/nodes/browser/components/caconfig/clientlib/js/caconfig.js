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
                this.$toolbar.find('.add').click(_.bind(this.createConfigurationResource, this));
                this.$toolbar.find('.remove').click(_.bind(this.deleteConfigurationResource, this));
                this.$content.on('click', '.create-configuration-resource', _.bind(this.createConfigurationResource, this));
                this.$content.on('click', '.create-configuration-collection-resource', _.bind(this.createConfigurationResource, this));
                this.$content.on('click', '.caconfig-property-editor', _.bind(this.openPropertyEditDialog, this));
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

            initContent: function () {
                this.$content.find('[data-toggle="tooltip"]').tooltip();
            },

            reload: function () {
                this.$el.addClass('loading');
                core.ajaxGet(core.getComposumPath('composum/nodes/browser/components/caconfig.content.html') +
                    core.encodePath(browser.getCurrentPath()),
                    {},
                    _.bind(function (content) {
                        this.$content.html(content);
                        this.initContent();
                    }, this), undefined, _.bind(function () {
                        this.$el.removeClass('loading');
                    }, this));
            },

            openPropertyEditDialog: function (event) {
                console.log('openPropertyEditDialog', arguments);
                var $target = $(event.currentTarget);
                var path = $target.data('path');
                var propertyName = $target.data('propertyname');
                var config = {
                    path: path,
                    name: propertyName,
                    type: $target.data('typename'),
                    value: $target.data('value'),
                    multi: $target.data('ismulti') == 'true'
                };
                core.openLoadedDialog(core.getComposumPath('composum/nodes/browser/components/caconfig/property.dialog.html') +
                    core.encodePath(path) + "?propertyName=" + propertyName, caconfig.PropertyEdit, config);
            },

        });

        // FIXME use browser.PropertyValueWidget
        caconfig.PropertyEdit = browser.PropertyDialog.extend({

            initialize: function (options) {
                browser.PropertyDialog.prototype.initialize.call(this, options);
                this.$type = this.$el.find('input[name="type"]');
                // this.$el.find('input[name="name"]').val(options.name);
                // this.$el.find('input[name="type"]').val(options.type);
                // this.$el.find('input[name="multi"]').prop('checked', options.multi);
                // this.$('button.apply').click(_.bind(function () {
                //     this.hide();
                // }, this));
                var oldtitle = this.$title.html(); // would be overwritten
                this.setProperty(new Map(Object.entries(options)));
                this.$title.html(oldtitle);
            }
        });

    })(CPM.nodes.browser.caconfig, CPM.nodes.browser, CPM.core);

})();
