(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.getDeleteAuthorizableDialog = function () {
            return core.getView('#authorizable-delete-dialog', usermanagement.DeleteAuthorizableDialog);
        };

        usermanagement.DeleteAuthorizableDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.authorizable = core.getWidget(this.el, 'input[name="authorizable"]', core.components.TextFieldWidget);
                this.$('button.delete').click(_.bind(this.deleteAuthorizable, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('button.delete').focus();
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            setUser: function (user) {
                this.authorizable.setValue(user);
            },

            deleteAuthorizable: function (event) {
                event.preventDefault();
                var path = usermanagement.getCurrentPath();

                core.ajaxDelete(
                    "/bin/cpm/usermanagement.authorizable.json" + path,
                    {
                        dataType: 'json'
                    },
                    _.bind(function(result) {
                        this.hide();
                        usermanagement.tree.refresh();
                    }, this),
                    _.bind(function(result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error deleting Authorizable', result);
                    }, this));
                return false;
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
