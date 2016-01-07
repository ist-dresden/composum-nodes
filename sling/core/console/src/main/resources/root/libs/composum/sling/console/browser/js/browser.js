/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.current = {};

        browser.getCurrentPath = function () {
            return browser.current ? browser.current.path : undefined;
        };

        browser.setCurrentPath = function (path) {
            if (!browser.current || browser.current.path != path) {
                if (path) {
                    core.getJson('/bin/core/node.tree.json' + path, undefined, undefined,
                        _.bind(function (result) {
                            browser.current = {
                                path: path,
                                node: result.responseJSON,
                                viewUrl: core.getContextUrl('/bin/browser.view.html' + window.core.encodePath(path)),
                                nodeUrl: core.getContextUrl('/bin/browser.html' + window.core.encodePath(path))
                            }
                            core.console.getProfile().set('browser', 'current', path);
                            if (history.replaceState) {
                                history.replaceState(browser.current.path, name, browser.current.nodeUrl);
                            }
                            $(document).trigger("path:selected", [path]);
                        }, this));
                } else {
                    browser.current = undefined;
                    $(document).trigger("path:selected", [path]);
                }
            }
        };

        browser.Browser = core.components.SplitView.extend({

            initialize: function (options) {
                core.components.SplitView.prototype.initialize.apply(this, [options]);
                $(document).on('path:select', _.bind(this.onPathSelect, this));
                $(document).on('path:selected', _.bind(this.onPathSelected, this));
            },

            onPathSelect: function (event, path) {
                if (!path) {
                    path = event.data.path;
                }
                browser.setCurrentPath(path);
            },

            onPathSelected: function (event, path) {
                browser.tree.selectNode(path, _.bind(function (path) {
                    browser.treeActions.refreshNodeState();
                }, this));
            }
        });

        browser.browser = core.getView('#browser', browser.Browser);

        browser.Tree = core.components.Tree.extend({

            nodeIdPrefix: 'BT_',

            initialize: function (options) {
                this.initialSelect = this.$el.attr('data-selected');
                if (!this.initialSelect || this.initialSelect == '/') {
                    this.initialSelect = core.console.getProfile().get('browser', 'current', "/");
                }
                this.filter = core.console.getProfile().get('browser', 'filter');
                core.components.Tree.prototype.initialize.apply(this, [options]);
            },

            dragAndDrop: {
                copy: false,
                is_draggable: function () {
                    return true;
                }
            },

            dropNode: function (draggedNode, targetNode) {
                var path = draggedNode.path;
                var dialog = core.nodes.getMoveNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setValues(draggedNode, targetNode);
                    this.selectNode(path);
                }, this));
            },

            dataUrlForPath: function (path) {
                var params = this.filter && 'default' != this.filter ? '?filter=' + this.filter : '';
                return '/bin/core/node.tree.json' + path + params;
            },

            onNodeSelected: function (path, node, element) {
                $(document).trigger("path:select", [path]);
                browser.treeActions.refreshNodeState();
            }
        });

        browser.tree = core.getView('#browser-tree', browser.Tree);

        browser.TreeActions = Backbone.View.extend({

            initialize: function (options) {
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
                core.getHtml('/bin/core/node.filters.html', _.bind(function (data) {
                    this.$filters.html(data);
                    this.$filters.find('li>a').click(_.bind(this.setFilter, this));
                    this.setFilterLabel(core.console.getProfile().get('browser', 'filter'));
                }, this));
                this.$('button.favorites').on('click', _.bind(this.toggleFavorites, this));
            },

            toggleFavorites: function (event) {
                return browser.favorites.toggleView(event);
            },

            setFilter: function (event) {
                event.preventDefault();
                var $link = $(event.currentTarget);
                var filter = $link.text();
                this.tree.setFilter(filter);
                this.setFilterLabel(filter);
                core.console.getProfile().set('browser', 'filter', filter);
            },

            setFilterLabel: function (filter) {
                this.$('.filter label span').text(filter);
                this.$filters.find('li').removeClass('active');
                var $active = this.$filters.find('li[data-filter="' + filter + '"]');
                $active.addClass('active');
            },

            refreshNodeState: function () {
                var node = this.tree.current();
                if (node && node.jcrState) {
                    this.$toggleLock.text(node.jcrState.locked ? 'Unlock' : 'Lock');
                    this.$toggleCheckout.text(node.jcrState.checkedOut ? 'Checkin' : 'Checkout');
                    if (node.jcrState.isVersionable) {
                        this.$toggleCheckout.parent().removeClass('disabled');
                    } else {
                        this.$toggleCheckout.parent().addClass('disabled');
                    }
                }
            },

            clipboardCopy: function (event) {
                var path = browser.getCurrentPath();
                core.console.getProfile().set('nodes', 'clipboard', {
                    path: path
                });
            },

            clipboardPaste: function (event) {
                var path = browser.getCurrentPath();
                var clipboard = core.console.getProfile().get('nodes', 'clipboard');
                if (path && clipboard && clipboard.path) {
                    core.ajaxPut("/bin/core/node.copy.json" + path, JSON.stringify({
                        path: clipboard.path
                    }), {
                        dataType: 'json'
                    }, _.bind(function (result) {
                        this.refreshTree();
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on copying node', result);
                    }, this));
                }
            },

            moveNode: function (event) {
                event.preventDefault();
                var dialog = core.nodes.getMoveNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setNode(this.tree.current());
                }, this));
            },

            renameNode: function (event) {
                event.preventDefault();
                var dialog = core.nodes.getRenameNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setNode(this.tree.current());
                }, this));
            },

            nodeMixins: function (event) {
                event.preventDefault();
                var dialog = core.nodes.getNodeMixinsDialog();
                dialog.show(_.bind(function () {
                    dialog.setNode(this.tree.current());
                }, this));
            },

            toggleCheckout: function (event) {
                event.preventDefault();
                var node = this.tree.current();
                if (node) {
                    //node.jcrState.checkedOut
                    core.ajaxPost('/bin/core/version.' + (node.jcrState.checkedOut ? 'checkin' : 'checkout') + '.json' + node.path,
                        {}, {}, _.bind(function (result) {
                            this.tree.refresh();
                        }, this), _.bind(function (result) {
                            core.alert('danger', 'Error', 'Error on toggle node lock', result);
                        }, this));
                }
            },

            toggleLock: function (event) {
                event.preventDefault();
                var node = this.tree.current();
                if (node) {
                    core.ajaxPost('/bin/core/node.toggle.lock' + node.path,
                        {}, {}, undefined, undefined, _.bind(function (result) {
                            if (result.status == 200) {
                                this.tree.refresh();
                            } else {
                                core.alert('danger', 'Error', 'Error on toggle node lock', result);
                            }
                        }, this));
                }
            },

            createNode: function (event) {
                var dialog = core.nodes.getCreateNodeDialog();
                dialog.show(_.bind(function () {
                    var parentNode = this.tree.current();
                    if (parentNode) {
                        dialog.initParentPath(parentNode.path);
                    }
                }, this));
            },

            deleteNode: function (event) {
                var dialog = core.nodes.getDeleteNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setNode(this.tree.current());
                }, this));

            },

            refreshTree: function (event) {
                this.tree.refresh();
            }
        });

        browser.treeActions = core.getView('.tree-actions', browser.TreeActions);

        browser.Breadcrumbs = Backbone.View.extend({

            initialize: function (options) {
                this.$('a').on('click', _.bind(this.pathSelected, this));
            },

            pathSelected: function (event) {
                event.preventDefault();
                var $target = $(event.currentTarget);
                var $row = $target.closest('li');
                var path = $row.attr('data-path');
                browser.setCurrentPath(path);
                return false;
            }
        });

        browser.getBreadcrumbs = function () {
            return core.getView('#browser-view .breadcrumbs', browser.Breadcrumbs);
        };

        /**
         * the base 'class' for all node tabs must be known before one of the special view implementations
         */
        browser.NodeTab = Backbone.View.extend({});

    })(core.browser);

})(window.core);
