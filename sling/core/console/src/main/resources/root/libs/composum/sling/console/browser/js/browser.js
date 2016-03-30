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
                            };
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
                $(document).on('path:changed', _.bind(this.onPathChanged, this));
            },

            onPathSelect: function (event, path) {
                if (!path) {
                    path = event.data.path;
                }
                browser.setCurrentPath(path);
            },

            onPathSelected: function (event, path) {
                browser.treeActions.refreshNodeState();
            },

            onPathChanged: function (event, path) {
                browser.treeActions.refreshNodeState();
            }
        });

        browser.browser = core.getView('#browser', browser.Browser);

        browser.Tree = core.components.Tree.extend({

            nodeIdPrefix: 'BT_',

            initialize: function (options) {
                options = _.extend(options || {}, {
                    dragAndDrop: {
                        is_draggable: _.bind(this.nodeIsDraggable, this),
                        inside_pos: 'last',
                        copy: false,
                        check_while_dragging: false,
                        drag_selection: false,
                        touch: 'selection',
                        large_drag_target: true,
                        large_drop_target: true,
                        use_html5: false
                    }
                });
                this.initialSelect = this.$el.attr('data-selected');
                if (!this.initialSelect || this.initialSelect == '/') {
                    this.initialSelect = core.console.getProfile().get('browser', 'current', "/");
                }
                this.filter = core.console.getProfile().get('browser', 'filter');
                core.components.Tree.prototype.initialize.apply(this, [options]);
            },

            nodeIsDraggable: function (selection, event) {
                return true;
            },

            dropNode: function (draggedNode, targetNode, index) {
                var parentPath = targetNode.path;
                var oldPath = draggedNode.path;
                var nodeName = core.getNameFromPath(oldPath);
                var newPath = parentPath + '/' + nodeName;
                core.ajaxPut('/bin/core/node.move.json' + core.encodePath(oldPath),
                    JSON.stringify({
                        path: parentPath,
                        name: nodeName,
                        index: index
                    }), {
                        dataType: 'json'
                    }, _.bind(function (data) {
                        $(document).trigger('path:moved', [oldPath, newPath]);
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on moving node!', result);
                    }, this));
            },

            dataUrlForPath: function (path) {
                var params = this.filter && 'default' != this.filter ? '?filter=' + this.filter : '';
                return '/bin/core/node.tree.json' + path + params;
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
                this.$('button.refresh').on('click', _.bind(this.refreshNode, this));
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
                if (browser.current) {
                    var node = browser.current.node;
                    if (node && node.jcrState) {
                        this.$toggleLock.text(node.jcrState.locked ? 'Unlock' : 'Lock');
                        this.$toggleCheckout.text(node.jcrState.checkedOut ? 'Checkin' : 'Checkout');
                        if (node.jcrState.isVersionable) {
                            this.$toggleCheckout.parent().removeClass('disabled');
                        } else {
                            this.$toggleCheckout.parent().addClass('disabled');
                        }
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
                    var dialog = core.nodes.getCopyNodeDialog();
                    dialog.show(_.bind(function () {
                        dialog.setNodePath(clipboard.path);
                        dialog.setTargetPath(path);
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
                            $(document).trigger('path:changed' [node.path]);
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
                                $(document).trigger('path:changed' [node.path]);
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
                event.preventDefault();
                var path = this.tree.getSelectedPath();
                if (path) {
                    var dialog = core.nodes.getDeleteNodeDialog();
                    dialog.show(_.bind(function () {
                        dialog.setPath(path);
                    }, this));
                }
            },

            refreshNode: function (event) {
                this.tree.refreshNodeById();
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

    })(core.browser);

})(window.core);
