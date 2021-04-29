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
                this.$toggleDisabled = this.$('.detail-toolbar .toggle-disabled');
                this.$editProfileButton = this.$('.detail-toolbar .edit-profile');
                this.$changePasswordButton = this.$('.detail-toolbar .change-password');
                this.$toggleDisabled.click(_.bind(this.toggleDisabled, this));
                this.$editProfileButton.click(_.bind(this.editProfile, this));
                this.$changePasswordButton.click(_.bind(this.changePassword, this));
                var current = usermanagement.current.node;
                if (current.disabled) {
                    this.$toggleDisabled.removeClass('fa-ban');
                    this.$toggleDisabled.addClass('fa-check-circle-o');
                    this.$toggleDisabled.attr('title', this.$toggleDisabled.data('title-enable'));
                } else {
                    this.$toggleDisabled.attr('title', this.$toggleDisabled.data('title-disable'));
                }
                if (current.systemUser) {
                    this.$changePasswordButton.addClass('disabled');
                }
            },

            toggleDisabled: function () {
                if (usermanagement.current.node.disabled) {
                    var path = usermanagement.current.node.name;
                    core.ajaxPost(
                        "/bin/cpm/usermanagement.enable.json/" + path,
                        {},
                        {
                            dataType: 'json'
                        },
                        _.bind(function () {
                            usermanagement.refreshState(usermanagement.getCurrentPath(), _.bind(this.refresh, this));
                        }, this),
                        _.bind(function (result) {
                            core.alert('danger', 'Error', 'Error on enable user', result);
                        }, this));
                } else {
                    var dialog = usermanagement.getDisableUserDialog();
                    dialog.show(function () {
                        dialog.setUser(usermanagement.current.node.name);
                    }, _.bind(function () {
                        usermanagement.refreshState(usermanagement.getCurrentPath(), _.bind(this.refresh, this));
                    }, this));
                }
            },

            enableUser: function () {
            },

            editProfile: function () {
                core.nodes.commons.user.openProfileDialog(usermanagement.getCurrentPath(),
                    undefined, undefined, _.bind(this.refresh, this));
            },

            changePassword: function () {
                var dialog = usermanagement.getChangePasswordDialog();
                dialog.show(function () {
                    dialog.setUser(usermanagement.current.node.name);
                }, _.bind(this.refresh, this));
            }
        });

        usermanagement.SystemUserTab = usermanagement.UserTab.extend({});

    })(CPM.nodes.usermanagement, CPM.core);

})();
