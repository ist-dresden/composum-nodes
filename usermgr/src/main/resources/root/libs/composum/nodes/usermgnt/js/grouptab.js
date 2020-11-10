(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.GroupTab = core.console.DetailTab.extend({

            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', usermanagement.UserTable);
                this.table.loadContent();
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
