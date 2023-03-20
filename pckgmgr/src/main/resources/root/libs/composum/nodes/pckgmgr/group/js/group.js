/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.pckgmgr');

    (function (pckgmgr, core) {

        pckgmgr.GroupTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$('.display-toolbar .cleanup').click(_.bind(pckgmgr[pckgmgr.mode.current].tree.actions.cleanupVersions, pckgmgr[pckgmgr.mode.current].tree.actions));
                this.$('.display-toolbar .refresh').click(_.bind(this.refresh, this));
            },

            refresh: function (event) {
                if (event) {
                    event.preventDefault();
                }
                pckgmgr.refresh();
            }

        });

    })(CPM.nodes.pckgmgr, CPM.core);

})();
