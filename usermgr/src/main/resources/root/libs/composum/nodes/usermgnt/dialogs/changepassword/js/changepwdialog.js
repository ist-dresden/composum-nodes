(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.getChangePasswordDialog = function () {
            return core.getView('#user-changepw-dialog', usermanagement.ChangePasswordDialog);
        };

        usermanagement.ChangePasswordDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$username = this.$('input[name="username"]');
                this.name = core.getWidget(this.el, 'input[name="username"]', core.components.TextFieldWidget);
                this.$password = this.$('input[name="password"]');
                this.$('button.create').click(_.bind(this.disableUser, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="password"]').focus();
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            setUser: function (user) {
                this.name.setValue(user);
            },

            disableUser: function (event) {
                event.preventDefault();
                var serializedData = this.form.$el.serialize();
                core.ajaxPost(
                    "/bin/cpm/usermanagement.password.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function(result) {
                        this.hide();
                    }, this),
                    _.bind(function(result) {
                        core.alert('danger', 'Error', 'Error changing password', result);
                    }, this));
                return false;
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
