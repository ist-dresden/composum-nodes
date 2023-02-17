'use strict';
/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.pckgmgr');

    (function (pckgmgr, core) {

        pckgmgr.getCreatePackageDialog = function () {
            return core.getView('#pckg-create-dialog', pckgmgr.CreatePackageDialog);
        };

        pckgmgr.getDeletePackageDialog = function () {
            return core.getView('#pckg-delete-dialog', pckgmgr.DeletePackageDialog);
        };

        pckgmgr.getUploadPackageDialog = function () {
            return core.getView('#pckg-upload-dialog', pckgmgr.UploadPackageDialog);
        };

        pckgmgr.getCleanupPackagesDialog = function () {
            return core.getView('#pckg-cleanup-dialog', pckgmgr.CleanupPackagesDialog);
        };

        pckgmgr.CreatePackageDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$group = this.$('input[name="group"]');
                this.$name = this.$('input[name="name"]');
                this.$version = this.$('input[name="version"]');
                this.$('button.create').click(_.bind(this.createPackage, this));
            },

            initGroup: function (group) {
                this.$group.val(group);
            },

            createPackage: function (event) {
                event.preventDefault();
                if (this.form.isValid()) {
                    this.submitForm(function (result) {
                        var path = result.path;
                        var parentPath = core.getParentPath(path);
                        var nodeName = core.getNameFromPath(path);
                        $(document).trigger("path:inserted", [parentPath, nodeName]);
                        $(document).trigger("path:select", [path]);
                    });
                } else {
                    this.alert('danger', 'a name must be specified');
                }
                return false;
            }
        });

        pckgmgr.DeletePackageDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$group = this.$('input[name="group"]');
                this.$name = this.$('input[name="name"]');
                this.$version = this.$('input[name="version"]');
                this.$registry = this.$('select[name="registry"]');
                this.$('button.delete').click(_.bind(this.deletePackage, this));
                this.$registry.parent().hide();
                this.isRegistryMode = undefined;
            },

            setPackage: function (pckg) {
                if (pckg) {
                    this.isRegistryMode = pckgmgr.mode.current == pckgmgr.const.mode.regpckg;
                    this.$group.val(pckg.group);
                    this.$name.val(pckg.name);
                    this.$version.val(pckg.version);
                    this.$registry.val(pckg.registry); // also falsy if in merged registry mode
                    this.registryVisible(pckg.registry);
                } else {
                    this.$group.val(undefined);
                    this.$name.val(undefined);
                    this.$version.val(undefined);
                    this.$registry.val(undefined);
                    this.registryVisible(false);
                    this.isRegistryMode = undefined;
                }
            },

            /** Sets visibility of dialog part for choosing the registry of the package. */
            registryVisible: function (visible) {
                if (visible) {
                    this.$registry.closest("div.form-group").show();
                    this.$registry.removeAttr('disabled');
                } else {
                    this.$registry.closest("div.form-group").hide();
                    this.$registry.attr('disabled','disabled');
                }
            },

            deletePackage: function (event) {
                event.preventDefault();
                var group = this.$group.val();
                var name = this.$name.val();
                var version = this.$version.val();
                var registry = this.$registry.val();
                var path;
                if (this.isRegistryMode) {
                    path = (registry ? '/@' + registry : '') + '/' + (group ? (group + '/') : '') + name + (version ? ('/' + version) : '/-');
                } else {
                    path = '/' + (group ? (group + '/') : '') + name + (version ? ('-' + version) : '') + '.zip';
                }
                if (this.form.isValid()) {
                    core.ajaxDelete("/bin/cpm/package.json" + core.encodePath(path), {},
                        _.bind(function (result) {
                            $(document).trigger('path:deleted', [path]);
                            this.hide();
                        }, this),
                        _.bind(function (result) {
                            this.alert('danger', 'Error on delete Package', result);
                        }, this)
                    );
                } else {
                    this.alert('danger', 'a valid Package must be specified');
                }
                return false;
            }
        });

        pckgmgr.UploadPackageDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$file = this.$('input[name="file"]');
                this.$file.on('change.file', _.bind(this.fileChanged, this));
                this.$merged = this.$('input[name="merged"]');
                this.$registry = this.$('input[name="registry"]');
                this.$('button.upload').click(_.bind(this.uploadPackage, this));
            },

            initDialog: function (path, merged) {
                this.isRegistryMode = pckgmgr.mode.current == pckgmgr.const.mode.regpckg;
                this.$merged.val(merged);
                this.$registry.prop("disabled", !this.isRegistryMode);
            },

            uploadPackage: function (event) {
                event.preventDefault();
                if (this.form.isValid()) {
                    this.submitForm(function (result) {
                        var path = result.path;
                        var parentPath = core.getParentPath(path);
                        var nodeName = core.getNameFromPath(path);
                        $(document).trigger("path:inserted", [parentPath, nodeName]);
                        $(document).trigger("path:select", [path]);
                    });
                } else {
                    if (this.form.getValues().registry != null) {
                        this.alert('danger', 'a file and a registry must be specified');
                    } else {
                        this.alert('danger', 'a file must be specified');
                    }
                }
                return false;
            },

            fileChanged: function () {
            }
        });

        pckgmgr.CleanupPackagesDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$path = this.$('input[name="path"]');
                this.$options = this.$('div.versioncheckboxes');
                this.$loadingmessage = this.$('div.loading-message');
                this.$cleanupbutton = this.$('button.cleanup');
                this.$cleanupbutton.click(_.bind(this.cleanupPackages, this));
                this.$cleanupbutton.prop('disabled', true);
                this.$options.empty();
            },

            setPackage: function (pckg) {
                this.$options.empty();
                if (pckg) {
                    this.$path.val(pckg.path);
                    this.loadOptions(pckg.path);
                    this.adjustSubmitButtonState();
                } else {
                    this.$path.val(undefined);
                }
            },

            loadOptions: function(path) {
                this.$loadingmessage.show();
                core.getHtml(
                    core.getContextUrl('/bin/packages.cleanupPackageOptions.html' + core.encodePath(path)),
                    _.bind(function (data) {
                        this.$options.html(data);
                        this.$loadingmessage.hide();
                        this.$checkboxes = this.$options.find('input.cleanup-version[type=checkbox]');
                        this.$checkboxes.click(_.bind(this.adjustSubmitButtonState, this));
                        this.adjustSubmitButtonState();
                    }, this));
            },

            /** The submit button shall only be active if at least one version is selected. */
            adjustSubmitButtonState: function() {
                this.$cleanupbutton.prop('disabled', !(this.$checkboxes && this.$checkboxes.is(':checked')));
            },

            cleanupPackages: function (event) {
                event.preventDefault();
                if (this.form.isValid()) {
                    this.submitForm(_.bind(function (result) {
                        var path = result.path;
                        $(document).trigger("path:changed", [path]);
                        if (result.data && result.data.result && result.data.result.deletedPaths) {
                            result.data.result.deletedPaths.forEach(
                                delpath => $(document).trigger("path:deleted", delpath)
                            );
                        } else {
                            this.alert('danger', 'BUG: no deleted paths?')
                        }
                        pckgmgr.refresh();
                    }, this), _.bind(this.onError, this));
                } else {
                    this.alert('danger', 'BUG: invalid form');
                }
                return false;
            }
        });

    })(CPM.nodes.pckgmgr, CPM.core);

})();
