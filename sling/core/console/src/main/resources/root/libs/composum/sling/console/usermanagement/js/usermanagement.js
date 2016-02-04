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
        return usermanagement.current ? usermanagement.current.path : undefined;
    };

    usermanagement.setCurrentPath = function(path) {
        if (! usermanagement.current || usermanagement.current.path != path) {
            if (path) {
                core.getJson ('/bin/core/usermanagement.tree.json' + path, undefined, undefined,
                    _.bind (function(result) {
                        usermanagement.current = {
                            path: path,
                            node: result.responseJSON,
                            viewUrl: '/libs/composum/sling/console/content/usermanagement.view.html' + window.core.encodePath(path),
                            nodeUrl: '/libs/composum/sling/console/content/usermanagement.html' + window.core.encodePath(path)
                        };

                        core.console.getProfile().set('usermanagement', 'current', path);
                        if (history.replaceState) {
                            history.replaceState (usermanagement.current.path, name, usermanagement.current.nodeUrl);
                        }
                        $(document).trigger("path:selected", [path]);
                    }, this));
            } else {
                usermanagement.current = undefined;
                $(document).trigger("path:selected", [path]);
            }
        }
    };

    usermanagement.Usermanagement = core.components.SplitView.extend({

        initialize: function(options) {
            core.components.SplitView.prototype.initialize.apply(this, [options]);
            $(document).on('path:select', _.bind(this.onPathSelect, this));
            $(document).on('path:selected', _.bind(this.onPathSelected, this));
        },

        onPathSelect: function(event, path) {
            if (!path) {
                path = event.data.path;
            }
            usermanagement.setCurrentPath(path);
        },

        onPathSelected: function(event, path) {
            usermanagement.tree.selectNode(path, _.bind(function(path) {
                usermanagement.treeActions.refreshNodeState();
            }, this));
        }
    });

    usermanagement.usermanagement = core.getView('#usermanagement', usermanagement.Usermanagement);

    usermanagement.Tree = core.components.Tree.extend({

        nodeIdPrefix: 'UT_',

        initialize: function(options) {
            this.initialSelect = this.$el.attr('data-selected');
            if (!this.initialSelect || this.initialSelect == '/') {
                this.initialSelect = core.console.getProfile().get('usermanagement', 'current', "/");
            }
            core.components.Tree.prototype.initialize.apply(this, [options]);
        },

        dataUrlForPath: function(path) {
            return '/bin/core/usermanagement.tree.json' + path;
        },

        onNodeSelected: function(path, node, element) {
            $(document).trigger("path:select", [path]);
        }

    });

    usermanagement.tree = core.getView('#usermanagement-tree', usermanagement.Tree);

    usermanagement.TreeActions = Backbone.View.extend({

        initialize: function(options) {
            this.tree = usermanagement.tree;
            this.table = core.getWidget(this.$el, '.table-container', usermanagement.UserTable);
            this.$('button.refresh').on('click', _.bind(this.refreshTree, this));
            this.$('button.adduser').on('click', _.bind(this.addUser, this));
            this.$('button.addgroup').on('click', _.bind(this.addGroup, this));
            this.$('button.deleteauthorizable').on('click', _.bind(this.deleteAuthorizable, this));
        },

        refreshNodeState: function() {
            var node = this.tree.current();
            if (node) {
                // todo
            }
        },

        refreshTree: function(event) {
            this.tree.refresh();
        },

        reload: function () {
            //this.table.loadContent();
        },

        addUser: function(event) {
            var dialog = usermanagement.getAddUserDialog();
            dialog.show(undefined, _.bind(this.reload, this));
        },

        deleteAuthorizable: function(event) {
            var path = usermanagement.getCurrentPath();
            core.ajaxDelete(
                "/bin/core/usermanagement.authorizable.json" + path,
                {
                    dataType: 'json'
                },
                _.bind(function(result) {
                    usermanagement.tree.refresh();
                }, this),
                _.bind(function(result) {
                    core.alert('danger', 'Error', 'Error deleting Authorizable', result);
                }, this));

        },

        addGroup: function(event) {
            var dialog = usermanagement.getAddGroupDialog();
            dialog.show(undefined, _.bind(this.reload, this));
        }

    });

    usermanagement.treeActions = core.getView('.tree-actions', usermanagement.TreeActions);


    usermanagement.UserTab = core.console.DetailTab.extend({

        initialize: function(options) {
            this.table = core.getWidget(this.$el, '.table-container', usermanagement.UserTable);
            this.table.loadContent();
            this.$disableUserButton = this.$('.table-toolbar .disable-user');
            this.$disableUserButton.click(_.bind(this.disableUser, this));
            this.$enableUserButton = this.$('.table-toolbar .enable-user');
            this.$enableUserButton.click(_.bind(this.enableUser, this));
            this.$changePasswordButton = this.$('.table-toolbar .change-password');
            this.$changePasswordButton.click(_.bind(this.changePassword, this));

        },

        reload: function () {
            this.table.loadContent();
        },

        disableUser: function () {
            var dialog = usermanagement.getDisableUserDialog();
            dialog.setUser(usermanagement.current.node.name);
            dialog.show(undefined, _.bind(this.reload, this));
        },

        changePassword: function () {
            var dialog = usermanagement.getChangePasswordDialog();
            dialog.setUser(usermanagement.current.node.name);
            dialog.show(undefined, _.bind(this.reload, this));
        },

        enableUser: function () {
            var path = usermanagement.current.node.name;
            core.ajaxPost(
                "/bin/core/usermanagement.enable.json/" + path,
                {

                },
                {
                    dataType: 'json'
                },
                _.bind(function(result) {
                    this.table.loadContent();
                }, this),
                _.bind(function(result) {
                    core.alert('danger', 'Error', 'Error on enable user', result);
                }, this));
        }

    });

    usermanagement.ProfileTab = core.console.DetailTab.extend({

        initialize: function(options) {
            this.table = core.getWidget(this.$el, '.table-container', usermanagement.PropertiesTable);
            this.table.loadContent("profile");
        }
    });

    usermanagement.PreferencesTab = core.console.DetailTab.extend({

        initialize: function(options) {
            this.table = core.getWidget(this.$el, '.table-container', usermanagement.PropertiesTable);
            this.table.loadContent("preferences");
        }
    });

    usermanagement.GroupsTab = core.console.DetailTab.extend({

        initialize: function(options) {
            this.table = core.getWidget(this.$el, '.table-container', usermanagement.GroupsTable);
            this.table.loadContent();
        }
    });

    usermanagement.GroupTab = core.console.DetailTab.extend({

        initialize: function(options) {
            this.table = core.getWidget(this.$el, '.table-container', usermanagement.UserTable);
            this.table.loadContent();
        }
    });


    //
    // detail view (console)
    //

    usermanagement.detailViewTabTypes = [{
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
        selector: '> .group',
        tabType: usermanagement.GroupTab
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
            return '/libs/composum/sling/console/content/usermanagement.tab.' + name + '.html';
        },

        getTabTypes: function () {
            return usermanagement.detailViewTabTypes;
        },

        initialize: function (options) {
            core.console.DetailView.prototype.initialize.apply(this, [options]);
        }
    });

    usermanagement.DetailView = core.getView('#usermanagement-view', usermanagement.DetailView);



})(core.usermanagement);

})(window.core);
