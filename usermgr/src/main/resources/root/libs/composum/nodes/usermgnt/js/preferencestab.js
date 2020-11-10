(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.PreferencesTab = core.console.DetailTab.extend({

            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', usermanagement.PropertiesTable);
                this.table.loadContent("preferences");
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
