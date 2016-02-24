(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {


        usermanagement.getChangePasswordDialog = function () {
            return core.getView('#user-changepw-dialog', usermanagement.ChangePasswordDialog);
        };


        usermanagement.ChangePasswordDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                //this.$name = this.$('input[name="username"]');
                this.name = core.getWidget(this.el, 'input[name="username"]', core.components.TextFieldWidget);
                this.password = this.$('input[name="password"]');
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
                var path = usermanagement.getCurrentPath();
                var serializedData = this.form.$el.serialize();
                core.ajaxPost(
                    "/bin/core/usermanagement.password.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function(result) {
                        this.hide();
                        //usermanagement.tree.refresh();
                    }, this),
                    _.bind(function(result) {
                        core.alert('danger', 'Error', 'Error changing password', result);
                    }, this));
                return false;
            }
        });


    })(core.usermanagement);

})(window.core);
