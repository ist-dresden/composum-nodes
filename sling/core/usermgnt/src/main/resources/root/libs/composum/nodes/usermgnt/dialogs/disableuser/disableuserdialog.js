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
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.name = core.getWidget(this.el, 'input[name="username"]', core.components.TextFieldWidget);
                this.$reason = this.$('input[name="reason"]');
                this.$('button.create').click(_.bind(this.disableUser, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="reason"]').focus();
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
                    "/bin/cpm/usermanagement.disable.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function(result) {
                        usermanagement.current.node.disabled = true;
                        usermanagement.tree.refresh();
                        this.hide();
                    }, this),
                    _.bind(function(result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error an disable user', result);
                    }, this));
                return false;
            }
        });

    })(core.usermanagement);

})(window.core);
