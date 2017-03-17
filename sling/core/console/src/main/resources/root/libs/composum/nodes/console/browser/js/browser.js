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

        browser.setCurrentPath = function (path, supressEvent) {
            if (!browser.current || browser.current.path != path) {
                if (path) {
                    browser.refreshCurrentPath(path, _.bind(function (result) {
                        core.console.getProfile().set('browser', 'current', path);
                        if (history.replaceState) {
                            history.replaceState(browser.current.path, name, browser.current.nodeUrl);
                        }
                        if (!supressEvent) {
                            $(document).trigger("path:selected", [path]);
                        }
                    }, this));
                } else {
                    browser.current = undefined;
                    if (!supressEvent) {
                        $(document).trigger("path:selected", [path]);
                    }
                }
            }
        };

        browser.refreshCurrentPath = function (path, callback) {
            if (!path && browser.current) {
                path = browser.current.path;
            }
            if (path) {
                core.getJson('/bin/cpm/nodes/node.tree.json' + path, undefined, undefined,
                    _.bind(function (result) {
                        browser.current = {
                            path: path,
                            node: result.responseJSON,
                            viewUrl: core.getContextUrl('/bin/browser.view.html' + window.core.encodePath(path)),
                            nodeUrl: core.getContextUrl('/bin/browser.html' + window.core.encodePath(path))
                        };
                        if (_.isFunction(callback)) {
                            callback.call(this, path);
                        }
                    }, this));
            }
        };

        browser.Browser = core.components.SplitView.extend({

            initialize: function (options) {
                core.components.SplitView.prototype.initialize.apply(this, [options]);
                $(document).on('path:select.Browser', _.bind(this.onPathSelect, this));
                $(document).on('path:selected.Browser', _.bind(this.onPathSelected, this));
                $(document).on('path:changed.Browser', _.bind(this.onPathChanged, this));
                core.unauthorizedDelegate = core.console.authorize;
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
                browser.refreshCurrentPath(path, _.bind(function () {
                    browser.treeActions.refreshNodeState();
                }, this));
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
                        touch: false, //'selection',
                        large_drag_target: true,
                        large_drop_target: true,
                        use_html5: false
                    }
                });
                this.initialSelect = this.$el.attr('data-selected');
                if (!this.initialSelect || this.initialSelect == '/') {
                    this.initialSelect = core.console.getProfile().get(this.getProfileId(), 'current', "/");
                }
                this.initializeFilter();
                core.components.Tree.prototype.initialize.apply(this, [options]);
                this.$jstree.on('keydown.BrowserTree', '.jstree-anchor', _.bind(this.customKeys, this));
            },

            getProfileId: function () {
                return 'browser'
            },

            initializeFilter: function () {
                this.filter = core.console.getProfile().get(this.getProfileId(), 'filter');
            },

            customKeys: function (event) {
                var tagName = event.target.tagName;
                if (tagName && tagName.toLowerCase() === "input") {
                    // interfere any input
                    return true;
                }
                var node = this.jstree.get_node(event.currentTarget.id);
                if (node) {
                    var path = node.original.path;
                    var keyCode = event.which;
                    switch (keyCode) {
                        case 68:  // D(elete)
                            if (!event.ctrlKey && !event.metaKey) {
                                break;
                            }
                        case 189: // '-'
                            event.preventDefault();
                            var nearest = this.findNearestOfDeletion(path);
                            browser.treeActions.deleteNode(undefined, path);
                            return false;
                        case 65:  // A(dd)
                            if (!event.ctrlKey && !event.metaKey) {
                                break;
                            }
                        case 187: // '+'
                            event.preventDefault();
                            browser.treeActions.createNode(undefined, path, _.bind(function () {
                                event.currentTarget.focus();
                            }, this));
                            return false;
                        case 67: // C(opy)
                            if (event.ctrlKey || event.metaKey) {
                                event.preventDefault();
                                browser.treeActions.clipboardCopy(undefined, path, _.bind(function () {
                                    event.currentTarget.focus();
                                }, this));
                                return false;
                            }
                            break;
                        case 86: // V(erbatim)
                            if (event.ctrlKey || event.metaKey) {
                                event.preventDefault();
                                browser.treeActions.clipboardPaste(undefined, path, _.bind(function () {
                                    event.currentTarget.focus();
                                }, this));
                                return false;
                            }
                            break;
                        case 82: // R(efresh)
                            if (event.ctrlKey || event.metaKey) {
                                event.preventDefault();
                                browser.treeActions.refreshNode(undefined, path, _.bind(function () {
                                    event.currentTarget.focus();
                                }, this));
                                return false;
                            }
                            break;
                    }
                }
                // let the others their chance
                return true;
            },

            nodeIsDraggable: function (selection, event) {
                return true;
            },

            renameNode: function (node, oldName, newName) {
                var nodePath = node.path;
                var parentPath = core.getParentPath(nodePath);
                var oldPath = core.buildContentPath(parentPath, oldName);
                var newPath = core.buildContentPath(parentPath, newName);
                core.ajaxPut('/bin/cpm/nodes/node.move.json' + core.encodePath(oldPath),
                    JSON.stringify({
                        path: parentPath,
                        name: newName
                    }), {
                        dataType: 'json'
                    }, _.bind(function (data) {
                        $(document).trigger('path:moved', [oldPath, newPath]);
                    }, this), _.bind(function (result) {
                        var dialog = core.nodes.getRenameNodeDialog();
                        dialog.show(_.bind(function () {
                            dialog.setNode(node, newName);
                            dialog.alert('danger', 'Error on renaming node!', result);
                        }, this));
                    }, this));
            },

            dropNode: function (draggedNode, targetNode, index) {
                var parentPath = targetNode.path;
                var oldPath = draggedNode.path;
                var nodeName = core.getNameFromPath(oldPath);
                var newPath = core.buildContentPath(parentPath, nodeName);
                core.ajaxPut('/bin/cpm/nodes/node.move.json' + core.encodePath(oldPath),
                    JSON.stringify({
                        path: parentPath,
                        name: nodeName,
                        index: index
                    }), {
                        dataType: 'json'
                    }, _.bind(function (data) {
                        $(document).trigger('path:moved', [oldPath, newPath]);
                    }, this), _.bind(function (result) {
                        var dialog = core.nodes.getMoveNodeDialog();
                        dialog.show(_.bind(function () {
                            dialog.setValues(draggedNode, targetNode, index);
                            dialog.alert('danger', 'Error on moving node!', result);
                        }, this));
                    }, this));
            },

            dataUrlForPath: function (path) {
                var params = this.filter && 'default' != this.filter ? '?filter=' + this.filter : '';
                return '/bin/cpm/nodes/node.tree.json' + path + params;
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
                core.getHtml('/bin/cpm/nodes/node.filters.html', _.bind(function (data) {
                    this.$filters.html(data);
                    this.$filters.find('li>a').click(_.bind(this.setFilter, this));
                    this.setFilterLabel(core.console.getProfile().get('browser', 'filter'));
                }, this));
                this.$('button.favorites').on('click', _.bind(this.toggleFavorites, this));
            },

            getCurrent: function () {
                return browser.current;
            },

            getCurrentPath: function () {
                return browser.getCurrentPath();
            },

            toggleFavorites: function (event) {
                return browser.navigation.toggleFavorites(event);
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
                var current = this.getCurrent();
                if (current) {
                    var node = current.node;
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

            clipboardCopy: function (event, path, callback) {
                if (event) {
                    event.preventDefault();
                }
                if (!path) {
                    path = this.getCurrentPath();
                }
                core.console.getProfile().set('nodes', 'clipboard', {
                    path: path
                });
                if (_.isFunction(callback)) {
                    callback.call(this, path);
                }
            },

            clipboardPaste: function (event, path, callback) {
                if (event) {
                    event.preventDefault();
                }
                if (!path) {
                    path = this.getCurrentPath();
                }
                var clipboard = core.console.getProfile().get('nodes', 'clipboard');
                if (path && clipboard && clipboard.path) {
                    var name = core.getNameFromPath(clipboard.path);
                    core.ajaxPut("/bin/cpm/nodes/node.copy.json" + path, JSON.stringify({
                        path: clipboard.path,
                        name: name
                    }), {
                        dataType: 'json'
                    }, _.bind(function (result) {
                        $(document).trigger('path:inserted', [path, name]);
                    }, this), _.bind(function (result) {
                        var dialog = core.nodes.getCopyNodeDialog();
                        dialog.show(_.bind(function () {
                            dialog.setNodePath(clipboard.path);
                            dialog.setTargetPath(path);
                            dialog.alert('danger', 'Error on copying node!', result);
                        }, this), _.bind(function () {
                            if (_.isFunction(callback)) {
                                callback.call(this, path);
                            }
                        }, this));
                    }, this));
                }
            },

            moveNode: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var dialog = core.nodes.getMoveNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setNode(this.tree.current());
                }, this));
            },

            renameNode: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var dialog = core.nodes.getRenameNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setNode(this.tree.current());
                }, this));
            },

            nodeMixins: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var dialog = core.nodes.getNodeMixinsDialog();
                dialog.show(_.bind(function () {
                    dialog.setNode(this.tree.current());
                }, this));
            },

            toggleCheckout: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var node = this.tree.current();
                if (node) {
                    //node.jcrState.checkedOut
                    core.ajaxPost('/bin/cpm/nodes/version.' + (node.jcrState.checkedOut ? 'checkin' : 'checkout') + '.json' + node.path,
                        {}, {}, _.bind(function (result) {
                            $(document).trigger('path:changed', [node.path]);
                        }, this), _.bind(function (result) {
                            core.alert('danger', 'Error', 'Error on toggle node lock', result);
                        }, this));
                }
            },

            toggleLock: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var node = this.tree.current();
                if (node) {
                    core.ajaxPost('/bin/cpm/nodes/node.toggle.lock' + node.path,
                        {}, {}, undefined, undefined, _.bind(function (result) {
                            if (result.status == 200) {
                                $(document).trigger('path:changed', [node.path]);
                            } else {
                                core.alert('danger', 'Error', 'Error on toggle node lock', result);
                            }
                        }, this));
                }
            },

            createNode: function (event, path, callback) {
                if (event) {
                    event.preventDefault();
                }
                if (!path) {
                    path = this.tree.getSelectedPath();
                }
                var dialog = core.nodes.getCreateNodeDialog();
                dialog.show(_.bind(function () {
                    if (path) {
                        dialog.initParentPath(path);
                    }
                }, this), _.bind(function () {
                    if (_.isFunction(callback)) {
                        callback.call(this, path);
                    }
                }, this));
            },

            deleteNode: function (event, path, callback) {
                if (event) {
                    event.preventDefault();
                }
                if (!path) {
                    path = this.tree.getSelectedPath();
                }
                if (path) {
                    var dialog = core.nodes.getDeleteNodeDialog();
                    dialog.show(_.bind(function () {
                        dialog.setPath(path);
                        if (event && (event.ctrlKey || event.metaKey)) {
                            // show dialog if clicked with 'ctrl' or 'meta'
                            dialog.setSmart(false);
                        }
                    }, this), _.bind(function () {
                        if (_.isFunction(callback)) {
                            callback.call(this, path);
                        }
                    }, this));
                }
            },

            refreshNode: function (event, path, callback) {
                if (event) {
                    event.preventDefault();
                }
                var id = undefined;
                if (path) {
                    id = this.tree.nodeId(path);
                }
                this.tree.refreshNodeById(id);
                if (_.isFunction(callback)) {
                    callback.call(this, path);
                }
            }
        });

        browser.treeActions = core.getView('#browser-tree-actions', browser.TreeActions);

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
