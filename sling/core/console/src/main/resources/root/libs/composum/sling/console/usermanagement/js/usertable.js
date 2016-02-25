(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {

        usermanagement.UserTable = Backbone.View.extend({
            initialize: function(options) {
                this.state = {
                    load: false
                };

                this.$table = this.$('.user-table');
                this.$table.bootstrapTable({
                    search: true,
                    showToggle: false,
                    striped: true,
                    singleSelect: true,
                    clickToSelect: true,

                    columns: [
                        {
                            class: 'name',
                            field: 'name',
                            title: 'Name',
                            width: '200px'
                        },
                        {
                            class: 'value',
                            field: 'value',
                            title: 'Value'
                        }]
                });

            },

            loadContent: function() {
                var path = usermanagement.current.node.name;
                var nodetype = usermanagement.current.node.type;
                this.state.load = true;
                core.ajaxGet(
                    "/bin/core/usermanagement." + nodetype + ".json/" + path,
                    {dataType: 'json'},
                    _.bind (function (result) {
                        var formattedResult = [
                            {'name':'id', 'value':result.id},
                            {'name':'path', 'value':result.path},
                            {'name':'principal name', 'value':result.principalName},
                            {'name':'member of', 'value':result.memberOf},
                            {'name':'declared member of', 'value':result.declaredMemberOf},
                            {'name':'members', 'value':result.members},
                            {'name':'declared members', 'value':result.declaredMembers}
                        ];
                        if (nodetype == 'user') {
                            formattedResult.push(
                                {'name': 'admin', 'value': result.admin},
                                {'name': 'system user', 'value': result.systemUser},
                                {'name': 'disabled', 'value': result.disabled},
                                {'name': 'disabled reason', 'value': result.disabledReason});
                        }
                        this.$table.bootstrapTable('load', formattedResult);
                    }, this),
                    _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error on loading properties', result);
                    }, this),
                    _.bind (function (result) {
                        this.state.load = false;
                    }, this)
                );
            }
        });

    })(core.usermanagement);

})(window.core);