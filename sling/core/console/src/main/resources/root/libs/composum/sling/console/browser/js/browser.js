/**
 *
 *
 */
(function(core) {
    'use strict';

    core.browser = core.browser || {};

(function(browser) {

    browser.current = {};

    browser.getCurrentPath = function() {
        return browser.current.path;
    };

    browser.setCurrentPath = function(path) {
        if (! browser.current || browser.current.path != path) {
            browser.current = {
                path: path,
                viewUrl: '/bin/browser.view.html' + window.core.encodePath(path),
                nodeUrl: '/bin/browser.html' + window.core.encodePath(path)
            }
            core.console.getProfile().set('browser', 'current', path);
            if (history.replaceState) {
                history.replaceState (browser.current.path, name, browser.current.nodeUrl);
            }
            browser.nodeView.reload();
        }
    };

    browser.selectNode = function(path) {
        browser.tree.selectNode(path);
    };

    browser.Browser = core.components.SplitView.extend({

        initialize: function(options) {
            core.components.SplitView.prototype.initialize.apply(this, [options]);
        }
    });

    browser.browser = core.getView('#browser', browser.Browser);

    browser.Tree = core.components.Tree.extend({

        nodeIdPrefix: 'BT_',

        initialize: function(options) {
            this.initialSelect = this.$el.attr('data-selected');
            if (!this.initialSelect || this.initialSelect == '/') {
                this.initialSelect = core.console.getProfile().get('browser', 'current', "/");
            }
            this.filter = core.console.getProfile().get('browser', 'filter');
            core.components.Tree.prototype.initialize.apply(this, [options]);
        },

        dragAndDrop: {
            copy: false,
            is_draggable: function() {
                return true;
            }
        },

        dropNode: function(draggedNode, targetNode) {
            var path = draggedNode.path;
            var dialog = core.nodes.getMoveNodeDialog();
            dialog.show();
            dialog.setValues(draggedNode, targetNode);
            this.selectNode (path);
        },

        dataUrlForPath: function(path) {
            var params = this.filter && 'default' != this.filter ? '?filter=' + this.filter : '';
            return '/bin/core/node.tree.json' + path + params;
        },

        onNodeSelected: function(path, node, element) {
            browser.setCurrentPath(path);
            browser.treeActions.refreshNodeState();
        },

        onInitialSelect: function(path) {
            browser.setCurrentPath (this.initialSelect);
        }
    });

    browser.tree = core.getView('#browser-tree', browser.Tree);

    browser.TreeActions = Backbone.View.extend({

        initialize: function(options) {
            this.tree = browser.tree;
            this.$toggleLock = this.$('a.lock');
            this.$toggleCheckout = this.$('a.checkout');
            this.$('a.move').on('click', _.bind(this.moveNode, this));
            this.$('a.rename').on('click', _.bind(this.renameNode, this));
            this.$('a.mixins').on('click', _.bind(this.nodeMixins, this));
            this.$toggleLock.on('click', _.bind(this.toggleLock, this));
            this.$toggleCheckout.on('click', _.bind(this.toggleCheckout, this));
            this.$('button.refresh').on('click', _.bind(this.refreshTree, this));
            this.$('button.create').on('click', _.bind(this.createNode, this));
            this.$('button.delete').on('click', _.bind(this.deleteNode, this));
            this.$('button.copy').on('click', _.bind(this.clipboardCopy, this));
            this.$('button.paste').on('click', _.bind(this.clipboardPaste, this));
            this.$filters = this.$('.filter .dropdown-menu');
            $.get('/bin/core/node.filters.html', _.bind(function(data) {
                this.$filters.html(data);
                this.$filters.find('li>a').click(_.bind(this.setFilter, this));
            }, this));
            this.setFilterLabel(core.console.getProfile().get('browser', 'filter'));
        },

        setFilter: function(event) {
            event.preventDefault();
            var $link = $(event.currentTarget);
            var filter = $link.text();
            this.tree.setFilter(filter);
            this.setFilterLabel(filter);
            core.console.getProfile().set('browser', 'filter', filter);
        },

        setFilterLabel: function(filter) {
            this.$('.filter label span').html(filter);
        },

        refreshNodeState: function() {
            var node = this.tree.current();
            if (node.jcrState) {
                this.$toggleLock.text (node.jcrState.locked ? 'Unlock' : 'Lock');
                this.$toggleCheckout.text (node.jcrState.checkedOut ? 'Ckeckin' : 'Checkout');
            }
        },

        clipboardCopy: function(event) {
            var path = browser.getCurrentPath();
            core.console.getProfile().set('nodes', 'clipboard', {
                path: path
            });
        },

        clipboardPaste: function(event) {
            var path = browser.getCurrentPath();
            var clipboard = core.console.getProfile().get('nodes', 'clipboard');
            if (path && clipboard && clipboard.path) {
                $.ajax({
                    url: "/bin/core/node.copy.json" + path,
                    data: JSON.stringify({
                        path: clipboard.path
                    }),
                    dataType: 'json',
                    type: 'PUT',
                    success: _.bind (function (result) {
                        this.refreshTree();
                    }, this),
                    error: _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error on copying node', result);
                    }, this)
                });
            }
        },

        moveNode: function(event) {
            event.preventDefault();
            var dialog = core.nodes.getMoveNodeDialog();
            dialog.show();
            dialog.setNode(this.tree.current());
        },

        renameNode: function(event) {
            event.preventDefault();
            var dialog = core.nodes.getRenameNodeDialog();
            dialog.show();
            dialog.setNode(this.tree.current());
        },

        nodeMixins: function(event) {
            event.preventDefault();
            var dialog = core.nodes.getNodeMixinsDialog();
            dialog.show();
            dialog.setNode(this.tree.current());
        },

        toggleCheckout: function(event) {
            event.preventDefault();
            var node = this.tree.current();
            if (node) {
                //node.jcrState.checkedOut
                $.ajax({
                    method: 'POST',
                    url: '/bin/core/version.' + (node.jcrState.checkedOut?'checkin':'checkout') + '.json' + node.path,
                    success: _.bind (function(result) {
                        this.tree.refresh();
                        core.browser.nodeView.reload();
                    }, this),
                    error: _.bind (function(result) {
                        core.alert('danger', 'Error', 'Error on toggle node lock', result);
                    }, this)
                });
            }
        },

        toggleLock: function(event) {
            event.preventDefault();
            var node = this.tree.current();
            if (node) {
                $.ajax({
                    method: 'POST',
                    url: '/bin/core/node.toggle.lock' + node.path,
                    complete: _.bind (function(result) {
                        if (result.status == 200) {
                            this.tree.refresh();
                        } else {
                            core.alert('danger', 'Error', 'Error on toggle node lock', result);
                        }
                    }, this)
                });
            }
        },

        createNode: function(event) {
            var dialog = core.nodes.getCreateNodeDialog();
            dialog.show();
            var parentNode = this.tree.current();
            if (parentNode) {
                dialog.initParentPath(parentNode.path);
            }
        },

        deleteNode: function(event) {
            var dialog = core.nodes.getDeleteNodeDialog();
            dialog.show();
            dialog.setNode(this.tree.current());
        },

        refreshTree: function(event) {
            this.tree.refresh();
        }
    });

    browser.treeActions = core.getView('.tree-actions', browser.TreeActions);

    browser.Breadcrumbs = Backbone.View.extend({

        initialize: function(options) {
            this.$('a').on('click', _.bind (this.pathSelected, this));
        },

        pathSelected: function(event) {
            event.preventDefault();
            var $target = $(event.currentTarget);
            var $row = $target.closest('li');
            var path = $row.attr('data-path');
            browser.selectNode(path);
            return false;
        }
    });

    browser.getBreadcrumbs = function(){
        return core.getView('#browser-view .breadcrumbs', browser.Breadcrumbs);
    };

    /**
     * the base 'class' for all node tabs must be known before one of the special view implementations
     */
    browser.NodeTab = Backbone.View.extend({
    });

})(core.browser);

})(window.core);
