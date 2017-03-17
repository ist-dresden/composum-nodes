(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {

        usermanagement.GroupsTable = Backbone.View.extend({
            initialize: function(options) {
                this.state = {
                    load: false
                };

                this.$table = this.$('.groups-table');
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
                            title: 'Authorizable "' + usermanagement.current.node.name + '" belongs to these group(s)'
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
                    "/bin/cpm/usermanagement.groupsofauthorizable.json/" + path,
                    {dataType: 'json'},
                    _.bind (function (result) {
                        var formattedResult = [];
                        for (var i = 0; i < result.length; i++) {
                            formattedResult.push(
                                {
                                    'name': result[i]
                                }
                            );
                        }
                        this.$table.bootstrapTable('load', formattedResult);
                    }, this),
                    _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error loading groups', result);
                    }, this),
                    _.bind (function (result) {
                        this.state.load = false;
                    }, this)

                );
            }

        });

    })(core.usermanagement);

})(window.core);