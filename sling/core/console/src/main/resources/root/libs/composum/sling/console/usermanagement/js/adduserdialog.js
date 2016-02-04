(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {


        usermanagement.getAddUserDialog = function () {
            return core.getView('#user-create-dialog', usermanagement.AddUserDialog);
        };


        usermanagement.AddUserDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$name = this.$('input[name="username"]');
                this.$password = this.$('input[name="password"]');
                this.$('button.create').click(_.bind(this.addNewUser, this));
            },

            reset: function () {
        ////                core.components.Dialog.prototype.reset.apply(this);
        //            this.$labelname.setValue("");
            },

            addNewUser: function (event) {
                event.preventDefault();
                var path = usermanagement.getCurrentPath();
                var serializedData = this.$form.$el.serialize();
                core.ajaxPost(
                    "/bin/core/usermanagement.user.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function(result) {
                        this.hide();
                        usermanagement.tree.refresh();
                    }, this),
                    _.bind(function(result) {
                        core.alert('danger', 'Error', 'Error creating group', result);
                    }, this));
                return false;
            }
        });


    })(core.usermanagement);

})(window.core);
