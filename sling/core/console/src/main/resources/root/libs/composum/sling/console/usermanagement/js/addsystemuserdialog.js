(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {


        usermanagement.getAddSystemUserDialog = function () {
            return core.getView('#systemuser-create-dialog', usermanagement.AddSystemUserDialog);
        };


        usermanagement.AddSystemUserDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$name = this.$('input[name="username"]');
                this.$('button.create').click(_.bind(this.addNewUser, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="username"]').focus();
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            addNewUser: function (event) {
                event.preventDefault();
                var serializedData = this.$form.$el.serialize();
                core.ajaxPost(
                    "/bin/core/usermanagement.systemuser.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function(result) {
                        this.hide();
                        usermanagement.tree.refresh(function() {
                            var path = JSON.parse(result.responseText).path;
                            $(document).trigger("path:select", [path]);
                        });
                    }, this),
                    _.bind(function(result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error creating user', result);
                    }, this));
                return false;
            }
        });


    })(core.usermanagement);

})(window.core);
