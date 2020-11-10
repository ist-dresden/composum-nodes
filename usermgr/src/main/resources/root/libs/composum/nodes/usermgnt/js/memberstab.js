(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.MembersTab = core.console.DetailTab.extend({

            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', usermanagement.MembersTable);
                this.table.loadContent();
                this.$addButton = this.$('.table-toolbar .add-authorizable-to-group');
                this.$addButton.click(_.bind(this.addMemberToThisToGroup, this));
                this.$removeButton = this.$('.table-toolbar .remove-authorizable-from-group');
                this.$removeButton.click(_.bind(this.removeMemberFromThisGroup, this));
            },

            reload: function () {
                this.table.loadContent();
            },

            addMemberToThisToGroup: function() {
                var dialog = usermanagement.getAddMemberDialog();
                dialog.show(function() {
                    dialog.setGroup(usermanagement.current.node.name);
                }, _.bind(this.reload, this));
            },

            removeMemberFromThisGroup: function() {
                var rows = this.table.getSelections();
                if (rows.length > 0) {
                    core.ajaxPut(
                        "/bin/cpm/usermanagement.removefromgroup.json",
                        JSON.stringify({
                            authorizable: (rows[0].name),
                            group: usermanagement.current.node.name
                        }), {
                            dataType: 'json'
                        },
                        _.bind(function (result) {
                            this.table.loadContent();
                        }, this),
                        _.bind(function (result) {
                            core.alert('danger', 'Error', 'Error removing authorizable from group', result);
                        }, this));
                }
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
