(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

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
                        this.hide();
                    }, this),
                    _.bind(function(result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error an disable user', result);
                    }, this));
                return false;
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
