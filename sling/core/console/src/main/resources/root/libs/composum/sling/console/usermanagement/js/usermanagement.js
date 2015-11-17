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
            this.$('button.refresh').on('click', _.bind(this.refreshTree, this));
        },

        refreshNodeState: function() {
            var node = this.tree.current();
            if (node) {
                // todo
            }
        },

        refreshTree: function(event) {
            this.tree.refresh();
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
                    debugger;
                    this.$nodeView = this.$('.node-view-panel');
                    this.$nodeTabs = this.$nodeView.find('.node-tabs');
                    this.$nodeContent = this.$nodeView.find('.node-view-content');
                }, this));
            } else {
                this.$el.html(''); // clear the view if nothing selected
            }
        }
    });

    usermanagement.authorizableView = core.getView('#usermanagement-view', usermanagement.AuthorizableView);



})(core.usermanagement);

})(window.core);
