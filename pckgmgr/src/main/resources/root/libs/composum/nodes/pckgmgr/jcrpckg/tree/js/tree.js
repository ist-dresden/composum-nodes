(function (jcrpckg, pckgmgr, console, core) {
    'use strict';

    jcrpckg.mode = {
        tree: {
            uri: function () {
                return '/bin/cpm/package.tree.json';
            },
            setup: function () {
                jcrpckg.tree = core.getView('#jcrpckg-tree', jcrpckg.Tree, {}, true);
            }
        }
    };

    jcrpckg.TreeActions = Backbone.View.extend({

        initialize: function (options) {
            this.tree = options.tree;
            this.$('button.refresh').on('click', _.bind(this.refreshTree, this));
            this.$('button.create').on('click', _.bind(this.createPackage, this));
            this.$('button.delete').on('click', _.bind(this.deletePackage, this));
            this.$('button.upload').on('click', _.bind(this.uploadPackage, this));
            this.$('button.cleanup').on('click', _.bind(this.cleanupVersions, this));
            this.$download = this.$('a.download');
        },

        refreshNodeState: function () {
            if (pckgmgr.current) {
                this.$download.attr('href', pckgmgr.current.downloadUrl);
            } else {
                this.$download.attr('href', '');
            }
        },

        createPackage: function (event) {
            var dialog = pckgmgr.getCreatePackageDialog();
            dialog.show(_.bind(function () {
                var parentNode = this.tree.current() || {};
                var parentPath = parentNode.path;
                if (parentNode.type === 'package') {
                    parentPath = core.getParentPath(parentPath);
                }
                if (parentPath) {
                    dialog.initGroup(parentPath.substring(1));
                }
            }, this));
        },

        deletePackage: function (event) {
            if (pckgmgr.current.name) {
                var dialog = pckgmgr.getDeletePackageDialog();
                dialog.show(_.bind(function () {
                    dialog.setPackage(pckgmgr.current);
                }, this));
            }
        },

        uploadPackage: function (event) {
            var dialog = pckgmgr.getUploadPackageDialog();
            dialog.show(_.bind(function () {
                dialog.initDialog(pckgmgr.current.path, '');
            }, this));
        },

        downloadPackage: function (event) {
        },

        refreshTree: function (event) {
            this.tree.refresh();
        },

        cleanupVersions: function (event) {
            var dialog = pckgmgr.getCleanupPackagesDialog();
            dialog.show(_.bind(function () {
                dialog.setPackage(pckgmgr.current);
            }, this));
        }
    });

    jcrpckg.Tree = core.components.Tree.extend({

        nodeIdPrefix: 'PMM_',

        initialize: function (options) {
            this.actions = core.getView('.nodes-pckgmgr-jcrpckg-tree .tree-actions', jcrpckg.TreeActions, {
                tree: this
            });
            this.initialSelect = this.$el.attr('data-selected');
            if (!this.initialSelect || this.initialSelect === '/') {
                this.initialSelect = core.console.getProfile().get('pckgmgr', 'current', "/");
            }
            this.filter = core.console.getProfile().get('jcrpckg', 'filter');
            core.components.Tree.prototype.initialize.apply(this, [options]);
        },

        dataUrlForPath: function (path) {
            return jcrpckg.mode.tree.uri() + path;
        },

        /** We cannot determine the package name uniquely from the path of the package, so we need to make a call. */
        parentPathsOfPath: function (path, rootPath, resultCallback) {
            var superMethod = core.components.Tree.prototype.parentPathsOfPath;
            if (path.indexOf('.') < 0) { // not a package, just use normal logic.
                superMethod.apply(this, [path, rootPath, resultCallback]);
            } else {
                this.nodeData({original: {path: path}}, function(result) {
                    if (result.parent) {
                        var parentPaths;
                        superMethod.apply(this, [result.parent, rootPath, function (parentResult) {
                            parentPaths = parentResult;
                        }]);
                        parentPaths.push(result.parent);
                        resultCallback(parentPaths);
                    } else { // not a package, normal logic
                        superMethod.apply(this, [path, rootPath, resultCallback]);
                    }
                });
            }
        },

        onNodeSelected: function (path, node, event) {
            $(document).trigger(core.makeEvent("path:select", undefined, event), [path])
        }
    });

})(CPM.namespace('nodes.pckgmgr.jcrpckg'), CPM.namespace('nodes.pckgmgr'), CPM.console, CPM.core);
