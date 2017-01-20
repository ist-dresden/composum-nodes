/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.PoliciesTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.verticalSplit = core.getWidget(this.$el, '.split-pane.vertical-split', core.components.VerticalSplitPane);
                this.localTable = core.getWidget(this.$el, '.local-policies > table', browser.PoliciesTable, {
                    selectable: true
                });
                this.effectiveTable = core.getWidget(this.$el, '.effective-policies > table', browser.PoliciesTable);
                this.$('.acl-toolbar .add').click(_.bind(this.addAcl, this));
                this.$('.acl-toolbar .remove').click(_.bind(this.removeSelection, this));
                this.$('.acl-toolbar .up').click(_.bind(this.up, this));
                this.$('.acl-toolbar .down').click(_.bind(this.down, this));
            },

            reload: function (callback) {
                this.loadTableData(this.localTable, 'local', callback);
                this.loadTableData(this.effectiveTable, 'effective');
            },

            loadTableData: function (table, scope, callback) {
                var path = browser.getCurrentPath();
                core.getJson('/bin/cpm/nodes/security.accessPolicies.' + scope + '.json' + path,
                    _.bind(function (result) {
                        var idx = 0;
                        _.each(result, function (aclEntry) {
                            aclEntry.index = idx;
                            idx++;
                        });
                        table.$el.bootstrapTable('load', result);
                    }, this),
                    _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on loading policies', result);
                    }, this),
                    _.bind(function (result) {
                        if (_.isFunction(callback)) {
                            callback(result);
                        }
                    }, this)
                );
            },

            addAcl: function (event) {
                browser.openAccessPolicyEntryDialog(_.bind(this.reload, this));
            },

            up: function (event) {
                var path = browser.getCurrentPath();
                var selected = this.localTable.getSelections();
                //var entries = [];
                //for (var i = 0; i < selected.length; i++) {
                //    entries[i] = selected[i];
                //}
                var firstSelected = selected[0];
                if (firstSelected && firstSelected.index > 0) {
                    var predecessor = this.localTable.getData(firstSelected.index - 1);
                    var successor = this.localTable.getData(firstSelected.index + 1);
                    core.ajaxPost("/bin/cpm/nodes/security.reorder.json" + path, {
                        //data
                        object: JSON.stringify(firstSelected),
                        before: JSON.stringify(predecessor)
                    }, {
                        //config
                        dataType: 'json'
                    }, _.bind(function (result) {
                        this.reload(_.bind(function () {
                            this.localTable.check(firstSelected.index - 1);
                        }, this));
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on reorder access policy entries', result);
                    }, this));
                }
            },

            down: function (event) {
                var path = browser.getCurrentPath();
                var selected = this.localTable.getSelections();
                //var entries = [];
                //for (var i = 0; i < selected.length; i++) {
                //    entries[i] = selected[i];
                //}
                var firstSelected = selected[0];
                var predecessor = this.localTable.getData(firstSelected.index - 1);
                var successor = this.localTable.getData(firstSelected.index + 1);
                if (firstSelected && firstSelected.index + 1 < this.localTable.numberOfRows()) {
                    core.ajaxPost("/bin/cpm/nodes/security.reorder.json" + path, {
                        //data
                        object: JSON.stringify(firstSelected),
                        before: JSON.stringify(successor)
                    }, {
                        //config
                        dataType: 'json'
                    }, _.bind(function (result) {
                        this.reload(_.bind(function () {
                            this.localTable.check(firstSelected.index + 1);
                        }, this));
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on reorder access policy entries', result);
                    }, this));
                }
            },

            removeSelection: function (event) {
                var path = browser.getCurrentPath();
                var selected = this.localTable.getSelections();
                var entries = [];
                for (var i = 0; i < selected.length; i++) {
                    entries[i] = selected[i];
                }
                if (path && entries) {
                    core.ajaxDelete("/bin/cpm/nodes/security.accessPolicy.json" + path, {
                        data: JSON.stringify(entries),
                        dataType: 'json'
                    }, _.bind(function (result) {
                        this.reload();
                    }, this), _.bind(function (result) {
                        if (result.status < 200 || result.status > 299) {
                            core.alert('danger', 'Error', 'Error on removing access policy entries', result);
                        } else {
                            this.reload();
                        }
                    }, this));
                }
            }
        });


    })(core.browser);

})(window.core);
