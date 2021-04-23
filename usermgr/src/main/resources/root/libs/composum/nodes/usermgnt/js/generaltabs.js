(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.GeneralTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$('.detail-toolbar .reload').click(_.bind(this.refresh, this));
                this.$('a.reference').click(_.bind(this.openReference, this));
            },

            refresh: function (event) {
                $(document).trigger("detail:reload");
            },

            openReference: function (event) {
                var $link = $(event.currentTarget);
                var path = $link.data('path');
                if (path) {
                    usermanagement.setCurrentPath(path);
                }
            }
        });

        usermanagement.GroupTab = usermanagement.GeneralTab.extend({

            initialize: function (options) {
                usermanagement.GeneralTab.prototype.initialize.call(this, options);
            }
        });

        usermanagement.ServiceUserTab = usermanagement.GeneralTab.extend({

            initialize: function (options) {
                usermanagement.GeneralTab.prototype.initialize.call(this, options);
            }
        });

        usermanagement.UserTab = usermanagement.GeneralTab.extend({

            initialize: function (options) {
                usermanagement.GeneralTab.prototype.initialize.call(this, options);
                this.$disableUserButton = this.$('.detail-toolbar .disable-user');
                this.$disableUserButton.click(_.bind(this.disableUser, this));
                this.$enableUserButton = this.$('.detail-toolbar .enable-user');
                this.$enableUserButton.click(_.bind(this.enableUser, this));
                this.$changePasswordButton = this.$('.detail-toolbar .change-password');
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

            disableUser: function () {
                var dialog = usermanagement.getDisableUserDialog();
                dialog.show(function () {
                    dialog.setUser(usermanagement.current.node.name);
                }, _.bind(this.refresh, this));
            },

            changePassword: function () {
                var dialog = usermanagement.getChangePasswordDialog();
                dialog.show(function () {
                    dialog.setUser(usermanagement.current.node.name);
                }, _.bind(this.refresh, this));
            },

            enableUser: function () {
                var path = usermanagement.current.node.name;
                core.ajaxPost(
                    "/bin/cpm/usermanagement.enable.json/" + path,
                    {},
                    {
                        dataType: 'json'
                    },
                    _.bind(function (result) {
                        this.refresh();
                    }, this),
                    _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on enable user', result);
                    }, this));
            }
        });

        usermanagement.SystemUserTab = usermanagement.UserTab.extend({});

    })(CPM.nodes.usermanagement, CPM.core);

})();
