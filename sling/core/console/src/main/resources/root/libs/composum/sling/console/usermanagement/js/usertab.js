(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {


        usermanagement.UserTab = core.console.DetailTab.extend({

            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', usermanagement.UserTable);
                this.table.loadContent();
                this.$disableUserButton = this.$('.table-toolbar .disable-user');
                this.$disableUserButton.click(_.bind(this.disableUser, this));
                this.$enableUserButton = this.$('.table-toolbar .enable-user');
                this.$enableUserButton.click(_.bind(this.enableUser, this));
                this.$changePasswordButton = this.$('.table-toolbar .change-password');
                this.$changePasswordButton.click(_.bind(this.changePassword, this));
                var current = usermanagement.current.node;
                if (current.disabled) {
                    this.$disableUserButton.addClass('disabled');
                    this.$enableUserButton.removeClass('disabled');
                } else {
                    this.$disableUserButton.removeClass('disabled');
                    this.$enableUserButton.addClass('disabled');
                }
                if (current.systemUser) {
                    this.$changePasswordButton.addClass('disabled');
                }
            },

            reload: function () {
                this.table.loadContent();
            },

            disableUser: function () {
                var dialog = usermanagement.getDisableUserDialog();
                dialog.setUser(usermanagement.current.node.name);
                dialog.show(undefined, _.bind(this.reload, this));
                this.$disableUserButton.addClass('disabled');
                this.$enableUserButton.removeClass('disabled');
            },

            changePassword: function () {
                var dialog = usermanagement.getChangePasswordDialog();
                dialog.setUser(usermanagement.current.node.name);
                dialog.show(undefined, _.bind(this.reload, this));
            },

            enableUser: function () {
                var path = usermanagement.current.node.name;
                core.ajaxPost(
                    "/bin/core/usermanagement.enable.json/" + path,
                    {

                    },
                    {
                        dataType: 'json'
                    },
                    _.bind(function(result) {
                        this.table.loadContent();
                        this.$disableUserButton.removeClass('disabled');
                        this.$enableUserButton.addClass('disabled');
                    }, this),
                    _.bind(function(result) {
                        core.alert('danger', 'Error', 'Error on enable user', result);
                    }, this));
            }

        });


    })(core.usermanagement);

})(window.core);
