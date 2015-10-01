(function(core) {
    'use strict';

    core.browser = core.browser || {};

    (function(browser) {

        browser.getVersionsTab = function() {
            return core.getView('.node-view-panel .versions', browser.VersionsTab);
        }


        browser.PropertiesTab = browser.NodeTab.extend({
            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', browser.VersionsTable);
            },

            reload: function() {
                this.table.loadContent();
            }
        })


        browser.VersionsTable = Backbone.View.extend({
            initialize: function(options) {

                this.$table = this.$('.version-table');
                this.$table.bootstrapTable({
                    columns: [{
                        class: 'name',
                        field: 'name',
                        title: 'Name'
                    },
                    {
                        class: 'date',
                        field: 'date',
                        title: 'Date'
                    }]
                });

            },

            loadContent: function() {
                var path = browser.getCurrentPath();
                this.state.load = true;
                $.ajax({
                    url: "/bin/core/version.versions.json" + path,
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

        })

    })(core.browser);

})(window.core);
