(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.PropertiesTable = Backbone.View.extend({
            initialize: function(options) {
                this.state = {
                    load: false
                };

                this.$table = this.$('.profile-table');
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

            loadContent: function(propertypath) {
                var path = usermanagement.current.node.name;
                this.state.load = true;

                core.ajaxGet(
                    "/bin/cpm/usermanagement.properties.json/" + path + "/" + propertypath,
                    {dataType: 'json'},
                    _.bind (function (result) {
                        this.$table.bootstrapTable('load', result);
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

    })(CPM.nodes.usermanagement, CPM.core);

})();
