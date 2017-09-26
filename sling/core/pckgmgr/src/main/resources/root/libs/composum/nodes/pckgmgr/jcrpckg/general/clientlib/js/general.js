/**
 *
 *
 */
(function (core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.getUpdatePackageDialog = function () {
            return core.getView('#pckg-update-dialog', pckgmgr.UpdatePackageDialog);
        };

        pckgmgr.UpdatePackageDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$path = this.$('input[name="path"]');
                this.$group = this.$('input[name="group"]');
                this.$name = this.$('input[name="name"]');
                this.$version = this.$('input[name="version"]');
                this.$description = this.$('textarea[name="jcr:description"]');
                this.includeVersions = core.getWidget(this.el, 'input[name="includeVersions"]', core.components.CheckboxWidget);
                this.$('button.save').click(_.bind(this.updatePackage, this));
            },

            initDialog: function (data) {
                var path = pckgmgr.getCurrentPath();
                if (data) {
                    if (data.path) {
                        path = data.path;
                    }
                    this.$group.val(data.group);
                    this.$name.val(data.name);
                    this.$version.val(data.version);
                    this.includeVersions.setValue(data.includeVersions);
                    this.$description.val(data.description);
                } else {
                    this.$group.val(undefined);
                    this.$name.val(undefined);
                    this.$version.val(undefined);
                    this.includeVersions.setValue(false);
                    this.$description.val(undefined);
                }
                this.$path.val(path);
                this.form.$el.attr('action', core.getContextUrl('/bin/cpm/package.update.json' + path));
            },

            updatePackage: function (event) {
                event.preventDefault();
                var oldPath = this.$path.val();
                if (this.form.isValid()) {
                    this.submitForm(function (result) {
                        pckgmgr.current.includeVersions = result.package.definition.includeVersions;
                        var newPath = result.path;
                        $(document).trigger('path:moved', [oldPath, newPath]);
                        $(document).trigger('path:changed', [newPath]);
                    });
                } else {
                    this.alert('danger', 'a name must be specified');
                }
                return false;
            }
        });

        pckgmgr.JcrPackageTab = core.console.JobControlTab.extend({

            jobTopic: 'com/composum/sling/core/pckgmgr/PackageJobExecutor',
            purgeAuditKeep: 6,

            initialize: function (options) {
                core.console.JobControlTab.prototype.initialize.apply(this, [options]);
                this.$header = this.$('.package-detail .header-view');
                this.$default = this.$('.aspect-view .default-aspect');
                this.$feedback = this.$('.aspect-view .feedback-aspect');
                this.$title = this.$feedback.find('.title');
                this.$('.display-toolbar .edit').click(_.bind(this.editPackage, this));
                this.$('.display-toolbar .install').click(_.bind(this.installPackage, this));
                this.$('.display-toolbar .assemble').click(_.bind(this.assemblePackage, this));
                this.$('.display-toolbar .uninstall').click(_.bind(this.uninstallPackage, this));
                this.$('.display-toolbar .upload').click(_.bind(pckgmgr.treeActions.uploadPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .create').click(_.bind(pckgmgr.treeActions.createPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .delete').click(_.bind(pckgmgr.treeActions.deletePackage, pckgmgr.treeActions));
                this.$('.display-toolbar .refresh').click(_.bind(this.refresh, this));
                this.$feedback.find('.close').click(_.bind(this.closeFeedback, this));
                this.$logOutput = this.$feedback.find('.feedback-display .log-output');
                this.$auditLog = this.$('.audit-log');
                this.$auditList = this.$auditLog.find('.audit-list');
                this.$auditLog.find('.toolbar .refresh').click(_.bind(this.loadAuditLog, this));
                this.$auditLog.find('.toolbar .purge').click(_.bind(this.purgeAuditLog, this));
                $(document).on('path:changed', _.bind(this.refresh, this));
            },

            getCurrentPath: function () {
                return pckgmgr.getCurrentPath();
            },

            editPackage: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var dialog = pckgmgr.getUpdatePackageDialog();
                dialog.show(_.bind(function () {
                    dialog.initDialog({
                        path: pckgmgr.current.path,
                        group: pckgmgr.current.group,
                        name: pckgmgr.current.name,
                        version: pckgmgr.current.version,
                        includeVersions: pckgmgr.current.includeVersions,
                        description: this.$('.package-detail .header-view .description').text()
                    });
                }, this));
            },

            startPackageOperation: function (event, title, operation) {
                if (event) {
                    event.preventDefault();
                }
                var dialog = core.console.getApprovalDialog();
                dialog.show(
                    _.bind(function () {
                        dialog.initDialog(
                            title,
                            '<div class="">'
                            + this.getCurrentPath()
                            + '</div>'
                        )
                    }, this), _.bind(function () {
                        this.startJob({
                            operation: operation
                        });
                    }, this));
            },

            installPackage: function (event) {
                this.startPackageOperation(event, 'Install Package', 'install');
            },

            assemblePackage: function (event) {
                this.startPackageOperation(event, 'Build Package', 'assemble');
            },

            uninstallPackage: function (event) {
                this.startPackageOperation(event, 'Uninstall Package', 'uninstall');
            },

            refresh: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.reload();
            },

            reload: function () {
                core.console.JobControlTab.prototype.reload.apply(this);
                core.ajaxGet(core.getContextUrl('/bin/packages.header.html' + pckgmgr.getCurrentPath()), {},
                    _.bind(function (data) {
                        this.$header.html(data);
                    }, this));
                this.loadAuditLog();
            },

            jobStarted: function (job) {
                core.console.JobControlTab.prototype.jobStarted.apply(this, [job]);
                this.openFeedback();
            },

            jobStopped: function () {
                core.console.JobControlTab.prototype.jobStopped.apply(this);
                this.reload();
            },

            jobSucceeded: function () {
                this.jobStopped();
            },

            closeFeedback: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.$feedback.addClass('hidden');
                this.$default.removeClass('hidden');
                this.resetAuditLog();
            },

            openFeedback: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.$default.addClass('hidden');
                this.$feedback.removeClass('hidden');
            },

            loadAuditLogfile: function (event) {
                this.openFeedback();
                core.console.JobControlTab.prototype.loadAuditLogfile.apply(this, [event]);
            }
        });

    })(core.pckgmgr);

})(window.core);
