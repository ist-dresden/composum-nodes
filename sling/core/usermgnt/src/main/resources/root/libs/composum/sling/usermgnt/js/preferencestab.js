(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {

        usermanagement.PreferencesTab = core.console.DetailTab.extend({

            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', usermanagement.PropertiesTable);
                this.table.loadContent("preferences");
            }
        });

    })(core.usermanagement);

})(window.core);
