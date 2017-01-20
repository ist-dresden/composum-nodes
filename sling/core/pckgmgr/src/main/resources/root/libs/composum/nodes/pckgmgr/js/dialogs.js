'use strict';
/**
 *
 *
 */
(function (core) {

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.getCreatePackageDialog = function () {
            return core.getView('#pckg-create-dialog', pckgmgr.CreatePackageDialog);
        };

        pckgmgr.getDeletePackageDialog = function () {
            return core.getView('#pckg-delete-dialog', pckgmgr.DeletePackageDialog);
        };

        pckgmgr.getUploadPackageDialog = function () {
            return core.getView('#pckg-upload-dialog', pckgmgr.UploadPackageDialog);
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
                this.$('button.delete').click(_.bind(this.deletePackage, this));
            },

            setPackage: function (pckg) {
                if (pckg) {
                    this.$group.val(pckg.group);
                    this.$name.val(pckg.name);
                    this.$version.val(pckg.version);
                } else {
                    this.$group.val(undefined);
                    this.$name.val(undefined);
                    this.$version.val(undefined);
                }
            },

            deletePackage: function (event) {
                event.preventDefault();
                var group = this.$group.val();
                var name = this.$name.val();
                var version = this.$version.val();
                var path = '/' + (group ? (group + '/') : '') + name + (version ? ('-' + version) : '') + '.zip';
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
                this.$('button.upload').click(_.bind(this.uploadPackage, this));
            },

            initDialog: function (path, name) {
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
                    this.alert('danger', 'a file must be specified');
                }
                return false;
            },

            fileChanged: function () {
            }
        });

    })(core.pckgmgr);
    
})(window.core);
