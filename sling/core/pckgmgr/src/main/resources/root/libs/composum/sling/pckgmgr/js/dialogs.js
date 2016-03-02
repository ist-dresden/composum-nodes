/**
 *
 *
 */
(function(core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

(function(pckgmgr) {

    pckgmgr.getCreatePackageDialog = function() {
        return core.getView('#pckg-create-dialog', pckgmgr.CreatePackageDialog);
    }

    pckgmgr.getDeletePackageDialog = function() {
        return core.getView('#pckg-delete-dialog', pckgmgr.DeletePackageDialog);
    }

    pckgmgr.getUploadPackageDialog = function() {
        return core.getView('#pckg-upload-dialog', pckgmgr.UploadPackageDialog);
    }

    pckgmgr.CreatePackageDialog = core.components.Dialog.extend({

        initialize: function(options) {
            core.components.Dialog.prototype.initialize.apply(this, [options]);
            this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
            this.$panel = this.$('.form-panel');
            this.$group = this.$('input[name="group"]');
            this.$name = this.$('input[name="name"]');
            this.$version = this.$('input[name="version"]');
            this.$('button.create').click(_.bind(this.createPackage, this));
        },

        initGroup: function(group) {
            this.$group.val(group);
        },

        createPackage: function(event) {
            event.preventDefault();
            if (this.$form.isValid()) {
                this.submitForm(function() {
                    pckgmgr.tree.refresh();
                });
            } else {
                this.alert ('danger', 'a group and name must be specified');
            }
            return false;
        }
    });

    pckgmgr.DeletePackageDialog = core.components.Dialog.extend({

        initialize: function(options) {
            core.components.Dialog.prototype.initialize.apply(this, [options]);
            this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
            this.$path = this.$('input[name="path"]');
            this.$('button.delete').click(_.bind(this.deletePackage, this));
        },

        setPackage: function(pckg) {
            this.$path.val(pckg.path);
        },

        deletePackage: function(event) {
            event.preventDefault();
            var path = this.$path.val();
            if (this.$form.isValid()) {
                $.ajax({
                    url: "/bin/core/package.json" + core.encodePath(path),
                    type: 'DELETE',
                    complete: _.bind (function (result) {
                        if (result.status == 200) {
                            this.hide();
                            pckgmgr.tree.refresh();
                        } else {
                            this.alert('danger', 'Error on delete Package', result);
                        }
                    }, this)
                });
            } else {
                this.alert('danger','a valid Package must be specified');
            }
            return false;
        }
    });

    pckgmgr.UploadPackageDialog = core.components.Dialog.extend({

        initialize: function(options) {
            core.components.Dialog.prototype.initialize.apply(this, [options]);
            this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
            this.$panel = this.$('.form-panel');
            this.$file = this.$('input[name="file"]');
            this.$file.on('change.file', _.bind(this.fileChanged, this));
            this.$('button.upload').click(_.bind(this.uploadPackage, this));
        },

        initDialog: function(path, name) {
        },

        uploadPackage: function(event) {
            event.preventDefault();
            if (this.$form.isValid()) {
                this.submitForm(function() {
                    pckgmgr.tree.refresh();
                });
            } else {
                this.alert ('danger', 'a file must be specified');
            }
            return false;
        },

        fileChanged: function() {
        }
    });

})(core.pckgmgr);

})(window.core);
