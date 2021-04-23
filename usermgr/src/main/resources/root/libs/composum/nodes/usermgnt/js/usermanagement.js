/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, console, core) {

        usermanagement.current = {};

        usermanagement.getCurrentPath = function () {
            return usermanagement.current ? usermanagement.current.path : undefined;
        };

        usermanagement.setCurrentPath = function (path) {
            if (!usermanagement.current || usermanagement.current.path !== path) {
                if (path) {
                    core.getJson('/bin/cpm/usermanagement.tree.json' + core.encodePath(path), undefined, undefined,
                        _.bind(function (result) {
                            usermanagement.current = {
                                path: path,
                                node: result.responseJSON,
                                viewUrl: core.getContextUrl('/bin/users.view.html' + core.encodePath(path)),
                                nodeUrl: core.getContextUrl('/bin/users.html' + core.encodePath(path))
                            };

                            core.console.getProfile().set('usermanagement', 'current', path);
                            if (history.replaceState) {
                                history.replaceState(usermanagement.current.path, name, usermanagement.current.nodeUrl);
                            }
                            $(document).trigger("path:selected", [path]);
                        }, this));
                } else {
                    usermanagement.current = undefined;
                    $(document).trigger("path:selected", [path]);
                }
            }
        };

        usermanagement.Usermanagement = console.components.SplitView.extend({

            initialize: function (options) {
                console.components.SplitView.prototype.initialize.apply(this, [options]);
                $(document).on('path:select', _.bind(this.onPathSelect, this));
                $(document).on('path:selected', _.bind(this.onPathSelected, this));
                $(document).on('path:changed', _.bind(this.onReloadTriggered, this));
                $(document).on('path:deleted', _.bind(this.onReloadTriggered, this));
                $(document).on('detail:reload', _.bind(this.onReloadTriggered, this));
                core.unauthorizedDelegate = core.console.authorize;
            },

            onPathSelect: function (event, path) {
                if (!path) {
                    path = event.data.path;
                }
                usermanagement.setCurrentPath(path);
            },

            onPathSelected: function (event, path) {
                usermanagement.tree.selectNode(path, _.bind(function (path) {
                    usermanagement.treeActions.refreshNodeState();
                }, this));
            },

            onReloadTriggered: function (event, path) {
                if (!usermanagement.detailView.busy) {
                    usermanagement.treeActions.refreshNodeState();
                    usermanagement.detailView.reload();
                }
            }
        });

        usermanagement.usermanagement = core.getView('#usermanagement', usermanagement.Usermanagement);

        usermanagement.Tree = core.components.Tree.extend({

            nodeIdPrefix: 'UT_',

            initialize: function (options) {
                this.initialSelect = this.$el.attr('data-selected');
                if (!this.initialSelect || this.initialSelect === '/' || this.initialSelect === '/home') {
                    this.initialSelect = core.console.getProfile().get('usermanagement', 'current', "/home");
                }
                this.rootPath = '/home';
                core.components.Tree.prototype.initialize.apply(this, [options]);
            },

            dataUrlForPath: function (path) {
                return '/bin/cpm/usermanagement.tree.json' + path;
            },

            onNodeSelected: function (path, node) {
                $(document).trigger("path:select", [path]);
            },

            refreshNodeState: function ($node, node) {
                core.components.Tree.prototype.refreshNodeState.apply(this, [$node, node]);
                if (node.original.systemUser) {
                    $node.addClass('systemuser');
                }
                return node;
            }
        });

        usermanagement.tree = core.getView('#usermanagement-tree', usermanagement.Tree);

        usermanagement.TreeActions = Backbone.View.extend({

            initialize: function (options) {
                this.tree = usermanagement.tree;
                this.table = core.getWidget(this.$el, '.table-container', usermanagement.UserTable);
                this.$('button.refresh').on('click', _.bind(this.refreshTree, this));
                this.$('button.adduser').on('click', _.bind(this.addUser, this));
                this.$('button.addsystemuser').on('click', _.bind(this.addSystemUser, this));
                this.$('button.addgroup').on('click', _.bind(this.addGroup, this));
                this.$('button.deleteauthorizable').on('click', _.bind(this.deleteAuthorizable, this));
            },

            refreshNodeState: function () {
                var node = this.tree.current();
                if (node) {
                    // todo
                }
            },

            refreshTree: function (event) {
                this.tree.refresh();
            },

            reload: function () {
            },

            addUser: function (event) {
                var dialog = usermanagement.getAddUserDialog();
                dialog.show(undefined, _.bind(this.reload, this));
            },

            addSystemUser: function (event) {
                var dialog = usermanagement.getAddSystemUserDialog();
                dialog.show(undefined, _.bind(this.reload, this));
            },

            deleteAuthorizable: function (event) {
                var path = usermanagement.getCurrentPath();
                var dialog = usermanagement.getDeleteAuthorizableDialog();
                dialog.show(function () {
                    dialog.setUser(path);
                }, _.bind(this.reload, this));
            },

            addGroup: function (event) {
                var dialog = usermanagement.getAddGroupDialog();
                dialog.show(undefined, _.bind(this.reload, this));
            }

        });

        usermanagement.treeActions = core.getView('.tree-actions', usermanagement.TreeActions);

        //
        // detail view (console)
        //

        usermanagement.detailViewTabTypes = [{
            selector: '> .group',
            tabType: usermanagement.GroupTab
        }, {
            selector: '> .service-user',
            tabType: usermanagement.ServiceUserTab
        }, {
            selector: '> .system-user',
            tabType: usermanagement.SystemUserTab
        }, {
            selector: '> .user',
            tabType: usermanagement.UserTab
        }, {
            selector: '> .profile',
            tabType: usermanagement.ProfileTab
        }, {
            selector: '> .preferences',
            tabType: usermanagement.PreferencesTab
        }, {
            // authorizable is member of groups listed here
            selector: '> .groups',
            tabType: usermanagement.GroupsTab
        }, {
            selector: '> .members',
            tabType: usermanagement.MembersTab
        }, {
            selector: '> .paths',
            tabType: usermanagement.PathsTab
        }, {
            selector: '> .graph',
            tabType: usermanagement.GraphTab
        }, {
            // the fallback to the basic implementation as a default rule
            selector: '> div',
            tabType: core.console.DetailTab
        }];

        /**
         * the node view (node detail) which controls the node view tabs
         */
        usermanagement.DetailView = core.console.DetailView.extend({

            getProfileId: function () {
                return 'usermanagement';
            },

            getCurrentPath: function () {
                return usermanagement.current ? usermanagement.current.path : undefined;
            },

            getViewUri: function () {
                return usermanagement.current.viewUrl;
            },

            getTabUri: function (name) {
                return '/bin/users.tab.' + name + '.html';
            },

            getTabTypes: function () {
                return usermanagement.detailViewTabTypes;
            },

            initialize: function (options) {
                core.console.DetailView.prototype.initialize.apply(this, [options]);
            }
        });

        usermanagement.detailView = core.getView('#usermanagement-view', usermanagement.DetailView);

    })(CPM.nodes.usermanagement, CPM.console, CPM.core);

})();
