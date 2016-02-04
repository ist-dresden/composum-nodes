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
                            class: 'name',
                            field: 'name',
                            title: 'Name'
                        }]
                });

            },

            loadContent: function() {
                var path = usermanagement.current.node.name;
                this.state.load = true;
                $.ajax({
                    url: "/bin/core/usermanagement.groupsofauthorizable.json/" + path,
                    dataType: 'json',
                    type: 'GET',
                    success: _.bind (function (result) {
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
                    error: _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error on loading groups', result);
                    }, this),
                    complete: _.bind (function (result) {
                        this.state.load = false;
                    }, this)
                });
            }

        });


    })(core.usermanagement);

})(window.core);