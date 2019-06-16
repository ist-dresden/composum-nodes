(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {

        usermanagement.GroupsTab = core.console.DetailTab.extend({

            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', usermanagement.GroupsTable);
                this.table.loadContent();
                this.$addButton = this.$('.table-toolbar .add-authorizable-to-group');
                this.$addButton.click(_.bind(this.addAuthorizableToGroup, this));
                this.$removeButton = this.$('.table-toolbar .remove-authorizable-from-group');
                this.$removeButton.click(_.bind(this.removeAuthorizableFromGroup, this));
            },

            reload: function () {
                this.table.loadContent();
            },

            addAuthorizableToGroup: function() {
                var dialog = usermanagement.getAddToGroupDialog();
                dialog.show(function() {
                    dialog.setUser(usermanagement.current.node.name);
                }, _.bind(this.reload, this));
            },

            removeAuthorizableFromGroup: function() {
                var rows = this.table.getSelections();
                if (rows.length > 0) {
                    core.ajaxPut(
                        "/bin/cpm/usermanagement.removefromgroup.json",
                        JSON.stringify({
                            authorizable: usermanagement.current.node.name,
                            group: (rows[0].name)
                        }), {
                            dataType: 'json'
                        },
                        _.bind(function (result) {
                            this.table.loadContent();
                        }, this),
                        _.bind(function (result) {
                            core.alert('danger', 'Error', 'Error removing user from group', result);
                        }, this));
                }
            }
        });

    })(core.usermanagement);

})(window.core);
