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
                    this.performReplicationCommand('activate', node.path, 'Activated');
                }
            },

            performReplicationCommand: function(cmd, path, title) {
                core.ajaxPost('/bin/replicate.json',
                    {
                        'path' : path,
                        'cmd': cmd
                    }, {}, _.bind(function (result) {
                        // result is HTML - does anybody know whether it's possible to get JSON from /bin/replicate.json?
                        var $result = $($.parseHTML(result));
                        var status = $result.find('#Status').text();
                        var message = $result.find('#Message').text();
                        if (status == '200' && message) {
                            core.alert('success', title, message);
                        } else {
                            core.alert('danger', 'Error ' + status, message ? message : 'Unparseable response on ' + cmd);
                        }
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on ' + cmd);
                    }, this));
            },

            deactivateNode: function (event) {
                if (event) {
                    event.preventDefault();
                }
                if (!$(event.currentTarget).hasClass('disabled')) {
                   var node = this.tree.current()
                    this.performReplicationCommand('deactivate', node.path, 'Deactivated');
                }
            }

        });

        aemBrowser.treeActions = core.getView('#browser-tree-actions', aemBrowser.TreeActions, null, true);

    })(CPM.nodes.aem.browser, CPM.nodes.browser, CPM.core);
})();
