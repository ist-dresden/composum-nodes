(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.getDeleteResourceDialog = function () {
            return core.getView('#resource-delete-dialog', usermanagement.DeleteResourceDialog);
        };

        usermanagement.DeleteResourceDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$name = this.$('.modal-body .approve .name');
                this.$('button.delete').click(_.bind(this.deleteResource, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('button.delete').focus();
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            setPath: function (path) {
                this.path = path;
                this.$name.text(core.getNameFromPath(path));
            },

            deleteResource: function (event) {
                event.preventDefault();
                if (this.path) {
                    core.ajaxDelete("/bin/cpm/nodes/node.json" + core.encodePath(this.path), {},
                        _.bind(function (result) {
                            this.hide();
                            $(document).trigger("path:deleted", this.path)
                        }, this),
                        _.bind(function (result) {
                            this.hide();
                            core.alert('danger', 'Error', 'Error deleting Resource', result);
                        }, this));
                }
                return false;
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
