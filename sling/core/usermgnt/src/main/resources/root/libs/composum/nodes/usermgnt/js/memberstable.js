(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {

        usermanagement.MembersTable = Backbone.View.extend({
            initialize: function(options) {
                this.state = {
                    load: false
                };

                this.$table = this.$('.members-table');
                this.$table.bootstrapTable({
                    search: true,
                    showToggle: false,
                    striped: true,
                    singleSelect: true,
                    clickToSelect: true,

                    columns: [
                        {
                            class: 'selection bs-checkbox',
                            checkbox: true,
                            sortable: false
                        }, {
                            class: 'name',
                            field: 'name',
                            title: 'Authorizable "' + usermanagement.current.node.name + '" has these member(s)'
                        }]
                });
            },

            getSelections: function () {
                return this.$table.bootstrapTable('getSelections');
            },

            loadContent: function() {
                var path = usermanagement.current.node.name;
                this.state.load = true;
                core.ajaxGet(
                    "/bin/cpm/usermanagement.group.json/" + path,
                    {dataType: 'json'},
                    _.bind (function (result) {
                        var formattedResult = [];
                        for (var i = 0; i < result.declaredMembers.length; i++) {
                            formattedResult.push(
                                {
                                    'name': result.declaredMembers[i]
                                }
                            );
                        }
                        this.$table.bootstrapTable('load', formattedResult);
                    }, this),
                    _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error on loading members', result);
                    }, this),
                    _.bind (function (result) {
                        this.state.load = false;
                    }, this)

                );
            }

        });


    })(core.usermanagement);

})(window.core);
