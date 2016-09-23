/**
 *
 *
 */
'use strict';

(function (core) {

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.pathPattern = /^\/((.+)\/)?([^\/]+)-([^\/]+)\.(zip|jar)$/;

        pckgmgr.current = {};

        pckgmgr.getCurrentPath = function () {
            return pckgmgr.current ? pckgmgr.current.path : undefined;
        };

        pckgmgr.setCurrentPath = function (path) {
            if (!pckgmgr.current || pckgmgr.current.path != path) {
                if (path) {
                    core.getJson('/bin/cpm/package.tree.json' + path, undefined, undefined,
                        _.bind(function (result) {
                            var pathMatch = pckgmgr.pathPattern.exec(path);
                            pckgmgr.current = {
                                path: path,
                                group: result.responseJSON.definition ? result.responseJSON.definition.group : undefined,
                                // group: pathMatch ? pathMatch[2] : undefined,
                                name: result.responseJSON.definition ? result.responseJSON.definition.name : undefined,
                                // name: pathMatch ? pathMatch[3] : undefined,
                                version: result.responseJSON.definition ? result.responseJSON.definition.version : undefined,
                                // version: pathMatch ? pathMatch[4] : undefined,
                                extension: pathMatch ? pathMatch[5] : undefined,
                                includeVersions: result.responseJSON.definition ? result.responseJSON.definition.includeVersions : undefined,
                                node: result.responseJSON,
                                viewUrl: core.getContextUrl('/bin/packages.view.html'
                                    + window.core.encodePath(path)),
                                nodeUrl: core.getContextUrl('/bin/packages.html'
                                    + window.core.encodePath(path)),
                                downloadUrl: pathMatch
                                    ? core.getContextUrl('/bin/cpm/package.download.zip' + path)
                                    : ''
                            };
                            core.console.getProfile().set('pckgmgr', 'current', path);
                            if (history.replaceState) {
                                history.replaceState(pckgmgr.current.path, name, pckgmgr.current.nodeUrl);
                            }
                            $(document).trigger("path:selected", [path]);
                        }, this));
                } else {
                    pckgmgr.current = undefined;
                    $(document).trigger("path:selected", [path]);
                }
            }
        };

        pckgmgr.Pckgmgr = core.components.SplitView.extend({

            initialize: function (options) {
                core.components.SplitView.prototype.initialize.apply(this, [options]);
                $(document).on('path:select', _.bind(this.onPathSelect, this));
                $(document).on('path:selected', _.bind(this.onPathSelected, this));
                core.unauthorizedDelegate = core.console.authorize;
            },

            onPathSelect: function (event, path) {
                if (!path) {
                    path = event.data.path;
                }
                pckgmgr.setCurrentPath(path);
            },

            onPathSelected: function (event, path) {
                pckgmgr.tree.selectNode(path, _.bind(function (path) {
                    pckgmgr.treeActions.refreshNodeState();
                }, this));
            }
        });

        pckgmgr.pckgmgr = core.getView('#pckgmgr', pckgmgr.Pckgmgr);

        pckgmgr.Tree = core.components.Tree.extend({

            nodeIdPrefix: 'PM_',

            initialize: function (options) {
                this.initialSelect = this.$el.attr('data-selected');
                if (!this.initialSelect || this.initialSelect == '/') {
                    this.initialSelect = core.console.getProfile().get('pckgmgr', 'current', "/");
                }
                this.filter = core.console.getProfile().get('pckgmgr', 'filter');
                core.components.Tree.prototype.initialize.apply(this, [options]);
            },

            dataUrlForPath: function (path) {
                return '/bin/cpm/package.tree.json' + path;
            },

            onNodeSelected: function (path, node, element) {
                $(document).trigger("path:select", [path]);
            }
        });

        pckgmgr.tree = core.getView('#package-tree', pckgmgr.Tree);

        pckgmgr.TreeActions = Backbone.View.extend({

            initialize: function (options) {
                this.tree = pckgmgr.tree;
                this.$('button.refresh').on('click', _.bind(this.refreshTree, this));
                this.$('button.create').on('click', _.bind(this.createPackage, this));
                this.$('button.delete').on('click', _.bind(this.deletePackage, this));
                this.$('button.upload').on('click', _.bind(this.uploadPackage, this));
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
                    var parentNode = this.tree.current();
                    var parentPath = parentNode.path;
                    if (parentNode.type == 'package') {
                        parentPath = core.getParentPath(parentPath);
                    }
                    if (parentNode) {
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
                }, this));
            },

            downloadPackage: function (event) {
            },

            refreshTree: function (event) {
                this.tree.refresh();
            }
        });

        pckgmgr.treeActions = core.getView('.tree-actions', pckgmgr.TreeActions);

        //
        // detail view (console)
        //

        pckgmgr.detailViewTabTypes = [{
            selector: '> .package',
            tabType: pckgmgr.JcrPackageTab
        }, {
            selector: '> .filters',
            tabType: pckgmgr.FiltersTab
        }, {
            selector: '> .coverage',
            tabType: pckgmgr.CoverageTab
        }, {
            selector: '> .options',
            tabType: pckgmgr.OptionsTab
        }, {
            selector: '> .group',
            tabType: pckgmgr.GroupTab
        }, {
            // the fallback to the basic implementation as a default rule
            selector: '> div',
            tabType: core.console.DetailTab
        }];

        /**
         * the node view (node detail) which controls the node view tabs
         */
        pckgmgr.DetailView = core.console.DetailView.extend({

            getProfileId: function () {
                return 'pckgmgr';
            },

            getCurrentPath: function () {
                return pckgmgr.current ? pckgmgr.current.path : undefined;
            },

            getViewUri: function () {
                return pckgmgr.current.viewUrl;
            },

            getTabUri: function (name) {
                return '/bin/packages.tab.' + name + '.html';
            },

            getTabTypes: function () {
                return pckgmgr.detailViewTabTypes;
            },

            initialize: function (options) {
                core.console.DetailView.prototype.initialize.apply(this, [options]);
            }
        });

        pckgmgr.detailView = core.getView('#pckgmgr-view', pckgmgr.DetailView);

    })(core.pckgmgr);

})(window.core);
