(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {


        usermanagement.getDisableUserDialog = function () {
            return core.getView('#user-disable-dialog', usermanagement.DisableUserDialog);
        };


        usermanagement.DisableUserDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                //this.$name = this.$('input[name="username"]');
                this.$name = core.getWidget(this.el, 'input[name="username"]', core.components.TextFieldWidget);
                this.$reason = this.$('input[name="reason"]');
                this.$('button.create').click(_.bind(this.disableUser, this));
                this.username = '';
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="reason"]').focus();
                });
            },

            reset: function () {
           //     this.$reason.setValue("no reason");
            },

            setUser: function (user) {
                this.$name.setValue(user);
                this.username = user;
            },

            disableUser: function (event) {
                event.preventDefault();
                var path = usermanagement.getCurrentPath();
                var serializedData = this.$form.$el.serialize();
                core.ajaxPost(
                    "/bin/core/usermanagement.disable.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function(result) {
                        this.hide();
                        usermanagement.tree.refresh();
                    }, this),
                    _.bind(function(result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error creating group', result);
                    }, this));
                return false;
            }
        });


    })(core.usermanagement);

})(window.core);
