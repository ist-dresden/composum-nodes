/**
 *
 *
 */
(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

(function(usermanagement) {

    usermanagement.getAddUserDialog = function () {
        return core.getView('#user-create-dialog', usermanagement.AddUserDialog);
    };

    usermanagement.getAddGroupDialog = function () {
        return core.getView('#group-create-dialog', usermanagement.AddGroupDialog);
    };

    usermanagement.AddUserDialog = core.components.Dialog.extend({

        initialize: function (options) {
            core.components.Dialog.prototype.initialize.apply(this, [options]);
            this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
            this.$name = this.$('input[name="username"]');
            this.$password = this.$('input[name="password"]');
            this.$('button.create').click(_.bind(this.addNewUser, this));
        },

        reset: function () {
////                core.components.Dialog.prototype.reset.apply(this);
//            this.$labelname.setValue("");
        },

        addNewUser: function (event) {
            event.preventDefault();
            var path = usermanagement.getCurrentPath();
            var serializedData = this.$form.$el.serialize();
            core.ajaxPost(
                "/bin/core/usermanagement.user.json",
                serializedData,
                {
                    dataType: 'post'
                },
                _.bind(function(result) {
                    this.hide();
                    usermanagement.tree.refresh();
                }, this),
                _.bind(function(result) {
                    core.alert('danger', 'Error', 'Error creating group', result);
                }, this));
            return false;
        }
    });

    usermanagement.AddGroupDialog = core.components.Dialog.extend({

        initialize: function (options) {
            core.components.Dialog.prototype.initialize.apply(this, [options]);
            this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
            //this.$name = this.$('input[name="name"]');
            this.$('button.create').click(_.bind(this.addNewGroup, this));
        },

        reset: function () {
        },

        addNewGroup: function (event) {
            event.preventDefault();
            var serializedData = this.$form.$el.serialize();
            core.ajaxPost(
                "/bin/core/usermanagement.group.json",
                serializedData,
                {
                    dataType: 'post'
                },
                _.bind(function(result) {
                    this.hide();
                    usermanagement.tree.refresh();
                }, this),
                _.bind(function(result) {
                    core.alert('danger', 'Error', 'Error creating group', result);
                }, this));
            return false;
        }
    });


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
            this.$('button.deleteauthorizable').on('click', _.bind(this.deleteAuthorizable, this));
            this.$('button.addgroup').on('click', _.bind(this.addGroup, this));
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


    usermanagement.AuthorizableView = Backbone.View.extend({
        initialize: function(options) {
            $(document).on('path:selected', _.bind(this.reload, this));
        },

        reload: function() {
            this.path = usermanagement.getCurrentPath();
            if (this.path) {
                // AJAX load for the current path with the 'viewUrl' from 'browser.current'
                this.$el.load(usermanagement.current.viewUrl, _.bind (function() {
                    this.userTable = core.getWidget(this.$el, '.table-container', usermanagement.UserTable);
                    this.userTable.loadContent();
                }, this));
            } else {
                this.$el.html(''); // clear the view if nothing selected
            }
        }
    });

    usermanagement.UserTable = Backbone.View.extend({
        initialize: function(options) {
            this.state = {
                load: false
            };

            this.$table = this.$('.user-table');
            this.$table.bootstrapTable({
                search: true,
                showToggle: false,
                striped: true,
                singleSelect: true,
                clickToSelect: true,

                columns: [
                    {
                        class: 'name',
                        field: 'name',
                        title: 'Name'
                    },
                    {
                        class: 'value',
                        field: 'value',
                        title: 'Value'
                    }]
            });

        },

        loadContent: function() {
            var path = usermanagement.current.node.name;
            var nodetype = usermanagement.current.node.type;
            this.state.load = true;
            $.ajax({
                url: "/bin/core/usermanagement." + nodetype + ".json/" + path,
                dataType: 'json',
                type: 'GET',
                success: _.bind (function (result) {
                    var formattedResult = [
                        {'name':'id', 'value':result.id},
                        {'name':'path', 'value':result.path},
                        {'name':'principal name', 'value':result.principalName},
                        {'name':'member of', 'value':result.memberOf},
                        {'name':'declared member of', 'value':result.declaredMemberOf}
                    ];
                    if (nodetype == 'user') {
                        formattedResult.push(
                            {'name': 'admin', 'value': result.admin},
                            {'name': 'disabled', 'value': result.disabled},
                            {'name': 'disabled reason', 'value': result.disabledReason});
                    }
                    this.$table.bootstrapTable('load', formattedResult);
                }, this),
                error: _.bind (function (result) {
                    core.alert ('danger', 'Error', 'Error on loading properties', result);
                }, this),
                complete: _.bind (function (result) {
                    this.state.load = false;
                }, this)
            });
        }

    });

    usermanagement.authorizableView = core.getView('#usermanagement-view', usermanagement.AuthorizableView);



})(core.usermanagement);

})(window.core);
