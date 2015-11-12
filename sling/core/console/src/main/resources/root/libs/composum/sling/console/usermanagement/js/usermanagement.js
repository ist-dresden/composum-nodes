/**
 *
 *
 */
(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

(function(usermanagement) {

    usermanagement.current = {};

    usermanagement.getCurrentPath = function() {
        return browser.current ? browser.current.path : undefined;
    };

    usermanagement.Tree = core.components.Tree.extend({

        nodeIdPrefix: 'BT_',

        initialize: function(options) {
            this.initialSelect = this.$el.attr('data-selected');
            if (!this.initialSelect || this.initialSelect == '/') {
                this.initialSelect = core.console.getProfile().get('usermanagement', 'current', "/");
            }
            core.components.Tree.prototype.initialize.apply(this, [options]);
        }

    });

    usermanagement.tree = core.getView('#usermanagement-tree', usermanagement.Tree);

})(core.usermanagement);

})(window.core);
