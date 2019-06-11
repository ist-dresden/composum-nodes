/**
 *
 *
 */
(function (core) {
    'use strict';

    core.console = core.console || {};

    (function (console) {

        console.Tree = core.components.Tree.extend({

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
                if (!this.initialSelect || this.initialSelect === '/') {
                    this.initialSelect = core.console.getProfile().get(this.getProfileId(), 'current', "/");
                }
                this.initializeFilter();
                core.components.Tree.prototype.initialize.apply(this, [options]);
                this.$jstree.on('keydown.BrowserTree', '.jstree-anchor', _.bind(this.customKeys, this));
            },

            onNodeSelected: function (path, node) {
                $(document).trigger("path:select", [path]);
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
                    // noinspection FallThroughInSwitchStatementJS
                    switch (keyCode) {
                        case 68:  // D(elete)
                            if (!event.ctrlKey && !event.metaKey) {
                                break;
                            }
                        case 189: // '-'
                            event.preventDefault();
                            var nearest = this.findNearestOfDeletion(path);
                            console.treeActions.deleteNode(undefined, path);
                            return false;
                        case 65:  // A(dd)
                            if (!event.ctrlKey && !event.metaKey) {
                                break;
                            }
                        case 187: // '+'
                            event.preventDefault();
                            console.treeActions.createNode(undefined, path, _.bind(function () {
                                event.currentTarget.focus();
                            }, this));
                            return false;
                        case 67: // C(opy)
                            if (event.ctrlKey || event.metaKey) {
                                event.preventDefault();
                                console.treeActions.clipboardCopy(undefined, path, _.bind(function () {
                                    event.currentTarget.focus();
                                }, this));
                                return false;
                            }
                            break;
                        case 86: // V(erbatim)
                            if (event.ctrlKey || event.metaKey) {
                                event.preventDefault();
                                console.treeActions.clipboardPaste(undefined, path, _.bind(function () {
                                    event.currentTarget.focus();
                                }, this));
                                return false;
                            }
                            break;
                        case 82: // R(efresh)
                            if (event.ctrlKey || event.metaKey) {
                                event.preventDefault();
                                console.treeActions.refreshNode(undefined, path, _.bind(function () {
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
                var beforePath = this.getNodePath(this.getNodeOfIndex(targetNode, index));
                var data = {
                    path: parentPath,
                    name: nodeName
                };
                if (beforePath) {
                    data.before = beforePath;
                } else {
                    data.index = -1;
                }
                core.ajaxPut('/bin/cpm/nodes/node.move.json' + core.encodePath(oldPath),
                    JSON.stringify(data), {
                        dataType: 'json'
                    }, _.bind(function (data) {
                        $(document).trigger('path:moved', [oldPath, newPath]);
                    }, this), _.bind(function (result) {
                        var dialog = core.nodes.getMoveNodeDialog();
                        dialog.show(_.bind(function () {
                            dialog.setValues(draggedNode, targetNode, beforePath, index);
                            dialog.alert('danger', 'Error on moving node!', result);
                        }, this));
                    }, this));
            }
        });

        console.TreeActions = Backbone.View.extend({

            initialize: function (options) {
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
                    this.setFilterLabel(core.console.getProfile().get('console', 'filter'));
                }, this));
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
                    core.ajaxPut("/bin/cpm/nodes/node.copy.json" + core.encodePath(path), JSON.stringify({
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
                            dialog.errorMessage('Copy Node', result);
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
                            if (result.status === 200) {
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

    })(core.console);

})(window.core);
