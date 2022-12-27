(function () {
    'use strict';
    CPM.namespace('nodes.aem.browser');
    (function (aemBrowser, browser, core) {

        aemBrowser.TreeActions = browser.TreeActions.extend({

            initialize: function (options) {
                this.tree = browser.tree;
                browser.TreeActions.prototype.initialize.apply(this, [options]);
                this.$('a.activate').on('click', _.bind(this.activateNode, this));
                this.$('a.deactivate').on('click', _.bind(this.deactivateNode, this));
            },

            activateNode: function (event) {
                if (event) {
                    event.preventDefault();
                }
                if (!$(event.currentTarget).hasClass('disabled')) {
                    var node = this.tree.current()
                    core.ajaxPost('/bin/replicate.json',
                        {
                            'path' : node.path,
                            'cmd':'activate'
                        }, {}, _.bind(function (result) {
                        }, this), _.bind(function (result) {
                            core.alert('danger', 'Error', 'Error on activate', result);
                        }, this));
                }
            },

            deactivateNode: function (event) {
                if (event) {
                    event.preventDefault();
                }
                if (!$(event.currentTarget).hasClass('disabled')) {
                   var node = this.tree.current()
                   core.ajaxPost('/bin/replicate.json',
                       {
                           'path' : node.path,
                           'cmd':'deactivate'
                       }, {}, _.bind(function (result) {
                       }, this), _.bind(function (result) {
                           core.alert('danger', 'Error', 'Error on deactivate', result);
                       }, this));
                }
            }


        });

        aemBrowser.treeActions = core.getView('#browser-tree-actions', aemBrowser.TreeActions, null, true);

    })(CPM.nodes.aem.browser, CPM.nodes.browser, CPM.core);
})();
