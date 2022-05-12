(function (regpckg, pckgmgr, console, core) {
    'use strict';

    regpckg.const = {
        uri: {
            tree: '/bin/cpm/package.registryTree.json'
        }
    };

    regpckg.mode = {
        current: '',
        profile: function () {
            return regpckg.mode.current === '' ? 'regpckg' : 'regmrgd';
        },
        tree: {
            uri: function () {
                return regpckg.mode.tree[regpckg.mode.current]();
            },
            '': function () {
                return '/bin/cpm/package.registryTree.json';
            },
            'merged': function () {
                return '/bin/cpm/package.registryTree.merged.json';
            },
            setup: function () {
                regpckg.tree = core.getView('#regpckg-tree', regpckg.Tree, {}, true);
            }
        }
    };

    regpckg.TreeActions = Backbone.View.extend({

        initialize: function (options) {
            this.tree = options.tree;
            this.$('button.refresh').on('click', _.bind(this.refreshTree, this));
            this.$('button.delete').on('click', _.bind(this.deletePackage, this));
            this.$('button.upload').on('click', _.bind(this.uploadPackage, this));
            this.$download = this.$('a.download');
            var merged = !!core.console.getProfile().get('regpckg', 'merged', false);
            this.$merged = this.$('.regpckg-mode-merged input');
            this.$merged.prop('checked', merged);
            this.$merged.change(_.bind(this.adjustMerged, this));
            regpckg.mode.current = merged ? 'merged' : '';
        },

        adjustMerged: function () {
            var merged = !!this.$merged.prop('checked');
            regpckg.mode.current = merged ? 'merged' : '';
            core.console.getProfile().set('regpckg', 'merged', merged);
            this.tree.initializeTree();
        },

        refreshNodeState: function () {
            if (pckgmgr.current) {
                this.$download.attr('href', pckgmgr.current.downloadUrl);
            } else {
                this.$download.attr('href', '');
            }
        },

        deletePackage: function (event) {
            if (pckgmgr.current.path) {
                var dialog = pckgmgr.getDeletePackageDialog();
                dialog.show(_.bind(function () {
                    dialog.setPackage(pckgmgr.current);
                }, this));
            }
        },

        uploadPackage: function (event) {
            var dialog = pckgmgr.getUploadPackageDialog();
            dialog.show(_.bind(function () {
            }, this));
        },

        downloadPackage: function (event) {
        },

        refreshTree: function (event) {
            this.tree.refresh();
        }
    });

    regpckg.Tree = core.components.Tree.extend({

        nodeIdPrefix: 'PMR_',

        initialize: function (options) {
            this.actions = core.getView('.nodes-pckgmgr-regpckg-tree .tree-actions', regpckg.TreeActions, {
                tree: this
            });
            this.filter = core.console.getProfile().get('regpckg', 'filter');
            this.initializeTree(options);
        },

        initializeTree: function (options) {
            if (this.jstree) {
                this.jstree.destroy();
            }
            this.initialSelect = this.$el.attr('data-selected');
            if (!this.initialSelect || this.initialSelect === '/') {
                this.initialSelect = core.console.getProfile().get(regpckg.mode.profile(), 'current', '/');
            }
            core.components.Tree.prototype.initialize.apply(this, [options || {}]);
        },

        dataUrlForPath: function (path) {
            return regpckg.mode.tree.uri() + path;
        },

        onNodeSelected: function (path, node) {
            $(document).trigger('path:select', [path]);
        }
    });

})(CPM.namespace('nodes.pckgmgr.regpckg'), CPM.namespace('nodes.pckgmgr'), CPM.console, CPM.core);
