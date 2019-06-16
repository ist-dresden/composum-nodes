/**
 *
 *
 */
(function (core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.EditOptionsDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$('button.save').click(_.bind(this.submitForm, this));
            },

            initDialog: function (options) {
                this.form.$el.attr('action', core.getContextUrl('/bin/cpm/package.update.json' + pckgmgr.getCurrentPath()));
                this.form.setValues(options);
            },

            submitForm: function (event) {
                event.preventDefault();
                this.submitFormPut();
            }
        });

        pckgmgr.getEditOptionsDialog = function () {
            return core.getView('#pckg-options-change-dialog', pckgmgr.EditOptionsDialog);
        };

        pckgmgr.EditRelationsDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.relations = core.getWidget(this.el, '.relations.widget', core.components.MultiFormWidget);
                this.$('button.save').click(_.bind(this.submitForm, this));
            },

            initDialog: function (name, title, values) {
                this.form.$el.attr('action', core.getContextUrl('/bin/cpm/package.update.json' + pckgmgr.getCurrentPath()));
                this.$('.modal-title').text(title);
                this.relations.$el.data('name', name);
                this.form.setValues(values);
            },

            submitForm: function (event) {
                event.preventDefault();
                this.submitFormPut();
            }
        });

        pckgmgr.getEditRelationsDialog = function () {
            return core.getView('#pckg-relations-change-dialog', pckgmgr.EditRelationsDialog);
        };

        pckgmgr.OptionsTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$detail = this.$('.options-detail');
                this.$('.detail-toolbar .advanced').click(_.bind(this.editOptions, this));
                this.$('.detail-toolbar .dependencies').click(_.bind(this.editDependencies, this));
                this.$('.detail-toolbar .replaces').click(_.bind(this.editReplaces, this));
                this.$('.detail-toolbar .refresh').click(_.bind(this.refresh, this));
            },

            editOptions: function (event) {
                event.preventDefault();
                var dialog = pckgmgr.getEditOptionsDialog();
                dialog.show(_.bind(function () {
                    dialog.initDialog({
                        acHandling: this.$('.ac-handling [data-value]').attr('data-value'),
                        requiresRoot: this.$('.requires-root [data-value]').attr('data-value'),
                        requiresRestart: this.$('.requires-restart [data-value]').attr('data-value'),
                        providerName: this.$('.provider-name .value-text').text(),
                        providerUrl: this.$('.provider-url .value-text').text(),
                        providerLink: this.$('.provider-link .value-text').text()
                    });
                }, this), _.bind(this.refresh, this));
            },

            editDependencies: function (event) {
                event.preventDefault();
                var dialog = pckgmgr.getEditRelationsDialog();
                dialog.show(_.bind(function () {
                    dialog.initDialog('dependencies', 'Change Dependencies', {
                        dependencies: this.relationsSet('dependencies')
                    });
                }, this), _.bind(this.refresh, this));
            },

            editReplaces: function (event) {
                event.preventDefault();
                var dialog = pckgmgr.getEditRelationsDialog();
                dialog.show(_.bind(function () {
                    dialog.initDialog('replaces', 'Change Replaces List', {
                        replaces: this.relationsSet('replaces')
                    });
                }, this), _.bind(this.refresh, this));
            },

            relationsSet: function (name) {
                var set = [];
                this.$('.' + name + ' .relation').each(function () {
                    set.push($(this).text());
                });
                return set;
            },

            refresh: function (event) {
                if (event) {
                    event.preventDefault();
                }
                core.ajaxGet(core.getContextUrl('/bin/packages.options.html' + pckgmgr.getCurrentPath()), {},
                    _.bind(function (data) {
                        this.$detail.html(data);
                    }, this));
            }
        });

    })(core.pckgmgr);
    
})(window.core);
