(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {

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
                var nodetype = usermanagement.current.node.type;
                //var propertypath = "profile";
                this.state.load = true;
                $.ajax({
                    url: "/bin/core/usermanagement.properties.json/" + path + "/" + propertypath,
                    dataType: 'json',
                    type: 'GET',
                    success: _.bind (function (result) {
                        this.$table.bootstrapTable('load', result);
                    }, this),
                    error: _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error on loading properties', result);
                    }, this),
                    complete: _.bind (function (result) {
                        this.state.load = false;
                    }, this)
                });
            }

        });


    })(core.usermanagement);

})(window.core);