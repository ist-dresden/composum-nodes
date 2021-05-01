(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.getAddGroupDialog = function () {
            return core.getView('#group-create-dialog', usermanagement.AddGroupDialog);
        };

        usermanagement.AddGroupDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$groupname = this.$('input[name="groupname"]');
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
                var serializedData = this.form.$el.serialize();
                core.ajaxPost(
                    "/bin/cpm/usermanagement.group.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function (result) {
                        this.hide();
                        var data = JSON.parse(result.responseText);
                        var path = data.path;
                        $(document).trigger('path:inserted', [core.getParentPath(path), core.getNameFromPath(path)]);
                        $(document).trigger("path:select", [path]);
                    }, this),
                    _.bind(function (result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error creating group', result);
                    }, this));
                return false;
            }
        });


    })(CPM.nodes.usermanagement, CPM.core);

})();
