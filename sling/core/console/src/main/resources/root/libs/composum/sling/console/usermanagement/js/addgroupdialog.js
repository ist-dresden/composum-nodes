(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {


        usermanagement.getAddGroupDialog = function () {
            return core.getView('#group-create-dialog', usermanagement.AddGroupDialog);
        };


        usermanagement.AddGroupDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                //this.$name = this.$('input[name="groupname"]');
                this.$('button.create').click(_.bind(this.addNewGroup, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="groupname"]').focus();
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            addNewGroup: function (event) {
                event.preventDefault();
                var serializedData = this.$form.$el.serialize();
                core.ajaxPost(
                    "/bin/core/usermanagement.group.json",
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
