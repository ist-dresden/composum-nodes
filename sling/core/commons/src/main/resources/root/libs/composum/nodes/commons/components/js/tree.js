/**
 *
 *
 */
(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components) {

        /**
         * the node type declarations for the tree (defines the icon classes of the nodes)
         */
        components.treeTypes = {
            'default': {'icon': 'fa fa-cube text-muted'},
            'summary': {'icon': 'fa fa-hand-o-right text-info'},
            'root': {'icon': 'fa fa-sitemap text-muted'},
            'system': {'icon': 'fa fa-cogs text-muted'},
            'activities': {'icon': 'fa fa-signal text-muted'},
            'nodetypes': {'icon': 'fa fa-tags text-muted'},
            'nodetype': {'icon': 'fa fa-tag text-muted'},
            'versionstorage': {'icon': 'fa fa-history text-muted'},
            'folder': {'icon': 'fa fa-folder-o'},
            'resource-folder': {'icon': 'fa fa-folder-o'},
            'orderedfolder': {'icon': 'fa fa-folder text-muted'},
            'package': {'icon': 'fa fa-file-archive-o'},
            'resource-package': {'icon': 'fa fa-file-archive-o'},
            'component': {'icon': 'fa fa-puzzle-piece text-info'},
            'container': {'icon': 'fa fa-cubes text-info'},
            'element': {'icon': 'fa fa-cube text-info'},
            'site': {'icon': 'fa fa-sitemap text-info'},
            'siteconfiguration': {'icon': 'fa fa-ellipsis-h text-muted'},
            'page': {'icon': 'fa fa-globe text-info'},
            'pagecontent': {'icon': 'fa fa-ellipsis-h text-muted'},
            'page-designer': {'icon': 'fa fa-cube text-info'},
            'resource-designer': {'icon': 'fa fa-file-code-o text-muted'},
            'resource-redirect': {'icon': 'fa fa-share text-info'},
            'resource-parsys': {'icon': 'fa fa-ellipsis-v text-muted'},
            'resource-console': {'icon': 'fa fa-laptop text-muted'},
            'resource-pckgmgr': {'icon': 'fa fa-laptop text-muted'},
            'resource-path': {'icon': 'fa fa-bookmark-o text-muted'},
            'resource-resources': {'icon': 'fa fa-filter text-muted'},
            'resource-strings': {'icon': 'fa fa-filter text-muted'},
            'resource-felix': {'icon': 'fa fa-cog text-muted'},
            'resource-guide': {'icon': 'fa fa-book text-muted'},
            'resource-servlet': {'icon': 'fa fa-cog text-muted'},
            'acl': {'icon': 'fa fa-key text-muted'},
            'authorizablefolder': {'icon': 'fa fa-diamond text-muted'},
            'group': {'icon': 'fa fa-group text-muted'},
            'user': {'icon': 'fa fa-user'},
            'linkedfile': {'icon': 'fa fa-link text-muted'},
            'file': {'icon': 'fa fa-file-o'},
            'resource': {'icon': 'fa fa-file-o'},
            'resource-file': {'icon': 'fa fa-file-o text-muted'},
            'file-image': {'icon': 'fa fa-file-image-o text-info'},
            'resource-image': {'icon': 'fa fa-file-image-o text-muted'},
            'file-video': {'icon': 'fa fa-file-video-o text-info'},
            'resource-video': {'icon': 'fa fa-file-video-o text-muted'},
            'file-text': {'icon': 'fa fa-file-text-o text-info'},
            'resource-text': {'icon': 'fa fa-file-text-o text-muted'},
            'file-text-plain': {'icon': 'fa fa-file-text-o text-info'},
            'resource-text-plain': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-html': {'icon': 'fa fa-globe text-info'},
            'resource-text-html': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-css': {'icon': 'fa fa-file-code-o text-info'},
            'resource-text-css': {'icon': 'fa fa-file-code-o text-muted'},
            'file-javascript': {'icon': 'fa fa-file-code-o text-info'},
            'resource-javascript': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-javascript': {'icon': 'fa fa-file-code-o text-info'},
            'resource-text-javascript': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-x-java-source': {'icon': 'fa fa-file-code-o text-info'},
            'resource-text-x-java-source': {'icon': 'fa fa-file-code-o text-muted'},
            'file-octet-stream': {'icon': 'fa fa-file-code-o text-info'},
            'resource-octet-stream': {'icon': 'fa fa-file-code-o text-muted'},
            'file-pdf': {'icon': 'fa fa-file-pdf-o text-info'},
            'resource-pdf': {'icon': 'fa fa-file-pdf-o text-muted'},
            'file-zip': {'icon': 'fa fa-file-archive-o text-info'},
            'resource-zip': {'icon': 'fa fa-file-archive-o text-muted'},
            'file-java-archive': {'icon': 'fa fa-file-archive-o text-info'},
            'resource-java-archive': {'icon': 'fa fa-file-archive-o text-muted'},
            'asset': {'icon': 'fa fa-picture-o text-info'},
            'assetcontent': {'icon': 'fa fa-picture-o text-muted'},
            'file-binary': {'icon': 'fa fa-file-o text-info'},
            'resource-binary': {'icon': 'fa fa-file-o text-muted'},
            'resource-syntheticresourceproviderresource': {'icon': 'fa fa-code text-muted'}
        };

        /**
         * A 'jstree' as Backbone View...
         *
         * Instance example:
         *
         *    core.pckgmgr.tree = new core.components.Tree({
         *
         *        el: $('.tree')[0], // the element to use
         *
         *        dataUrlForPath: function(path) {
         *            return '/bin/cpm/nodes/node.page.title.json' + path;
         *        },
         *
         *        onNodeSelected: function(path) {
         *            alert('onNodeSelected: ' + path);
         *        }
         *    });
         */
        components.Tree = Backbone.View.extend({

            /**
             * returns the model data of the selected node; 'undefined' if no node is selected
             * @deprecated use getSelectedPath() or getSelectedTreeNode() instead of current()
             */
            current: function () {
                var selectedTreeNode = this.getSelectedTreeNode();
                return selectedTreeNode ? selectedTreeNode.original : undefined;
            },

            /**
             * @returns the path of the resource selected currently; {undefined} if nothing selected
             */
            getSelectedPath: function () {
                var selectedTreeNode = this.getSelectedTreeNode();
                return selectedTreeNode ? selectedTreeNode.original.path : undefined;
            },

            /**
             * @returns the jsTree node object selected currently; {undefined} if nothing selected
             */
            getSelectedTreeNode: function () {
                var $selection = this.jstree.get_selected(true);
                return $selection && $selection.length > 0 ? $selection[0] : undefined;
            },

            /**
             * @returns the jsTree node object which represents the path
             */
            getTreeNode: function (path) {
                var id = this.nodeId(path);
                return this.jstree.get_node(id);
            },


            /**
             * @returns the jsTree jQuery (DOM) object which represents the path
             */
            get$TreeNode: function (path) {
                var id = this.nodeId(path);
                return this.jstree.get_node(id, true);
            },

            /**
             * @returns the jsTree jQuery (DOM) anchor object of the path
             */
            get$TreeNodeAnchor: function (path) {
                var $node = this.get$TreeNode(path);
                var $anchor = $node.children('.jstree-anchor');
                return $anchor;
            },

            /**
             * refreshes the tree view, clears all cached tree structure and reopens the selected node
             */
            refresh: function (callback) {
                var current = this.current();
                this.delegate('refresh.jstree', _.bind(function () {
                    this.undelegate('refresh.jstree');
                    if (_.isFunction(callback)) {
                        callback(current);
                    } else {
                        if (current) {
                            this.selectNode(current.path, undefined, true);
                        }
                    }
                }, this));
                this.jstree.refresh(true, true);
            },

            /**
             * selects the node specified by its node path if the node is accepted by the filter
             * opens all nodes up to the target node automatically
             */
            selectNode: function (path, callback, supressEvent) {
                this.resetSelection();
                if (path) {
                    var names = $.isArray(path) ? path : path.split('/');
                    var index = 1;
                    var $node;
                    var drilldown = function () {
                        if (index < names.length - 1) {
                            var id = this.nodeId(_.first(names, index + 1));
                            $node = this.$('#' + id);
                            index++;
                            if ($node) {
                                if (this.jstree.is_open($node)) {
                                    drilldown.apply(this);
                                } else {
                                    if (!this.jstree.is_leaf($node)) {
                                        this.jstree.open_node($node);
                                    } else {
                                        this.undelegate('after_open.jstree');
                                        if (_.isFunction(callback)) {
                                            callback(path);
                                        }
                                    }
                                }
                            } else {
                                this.undelegate('after_open.jstree');
                                if (_.isFunction(callback)) {
                                    callback(path);
                                }
                            }
                        } else {
                            var id = this.nodeId(_.first(names, names.length));
                            $node = this.$('#' + id);
                            if ($node) {
                                this.jstree.select_node($node, supressEvent);
                                this.scrollIntoView($node);
                            }
                            this.undelegate('after_open.jstree');
                            if (_.isFunction(callback)) {
                                callback(path);
                            }
                        }
                    };
                    this.delegate('after_open.jstree', undefined, _.bind(drilldown, this));
                    $node = this.$('#' + this.nodeId(this.getRootPath()));
                    if ($node) {
                        if (this.jstree.is_open($node)) {
                            drilldown.apply(this);
                        } else {
                            if (!this.jstree.is_leaf($node)) {
                                this.jstree.open_node($node);
                            } else {
                                this.undelegate('after_open.jstree');
                                if (_.isFunction(callback)) {
                                    callback(path);
                                }
                            }
                        }
                    }
                }
            },

            resetSelection: function () {
                this.jstree.deselect_all();
            },

            reset: function () {
                this.resetSelection();
            },

            /**
             * scrolls the trees viewport that the specified node (jQuery element of the node)
             * is visible within the viewport (scrolls if necessary - node not visible - only)
             */
            scrollIntoView: function ($node) {
                var $panel = this.$jstree.closest('.tree-panel');
                var nodePos = $node.position();
                if (nodePos) {
                    var nodeTop = nodePos.top;
                    var scrollTop = $panel.scrollTop();
                    var scrollHeight = $panel.height();
                    if (nodeTop < scrollTop + scrollHeight / 5) {
                        $panel.scrollTop(nodeTop - scrollHeight / 4);
                    } else if (nodeTop > scrollTop + scrollHeight - scrollHeight / 5) {
                        $panel.scrollTop(nodeTop - scrollHeight + scrollHeight / 4);
                    }
                }
            },

            // tree initialization

            /**
             * sets the root path for the tree
             */
            setRootPath: function (rootPath, refresh) {
                this.rootPath = rootPath;
                if (refresh === undefined || refresh) {
                    this.refresh();
                }
            },

            getRootPath: function () {
                return this.rootPath ? this.rootPath : '/';
            },

            /**
             * sets the filter key for the tree; the key is the 'name' in the filter OSGi configuration)
             */
            setFilter: function (filter, refresh) {
                this.filter = filter;
                if (refresh === undefined || refresh) {
                    this.refresh();
                }
            },

            initialize: function (options) {

                var treeOptions = {

                    'plugins': [
                        'types',
                        'unique',
                        'wholerow'
                    ],
                    'core': {
                        'animation': false,
                        'check_callback': _.bind(this.checkCallback, this),
                        'data': _.bind(this.nodeData, this),
                        'cache': false,
                        'load_open': true,
                        'multiple': false,
                        'themes': {
                            'name': 'proton'
                        },
                        'force_text': true
                    },
                    'types': components.treeTypes
                };

                // extend initialization to set up the drag and drop functionality if configured in the options
                if (options.dragAndDrop) {
                    this.dragAndDrop = _.extend(this.dragAndDrop || {}, options.dragAndDrop);
                }
                if (this.dragAndDrop) {
                    treeOptions.plugins = _.union(treeOptions.plugins, ['dnd']);
                    treeOptions.dnd = this.dragAndDrop;
                }

                //  extend initialization to set up a context menu if configured in the options
                if (options.contextmenu) {
                    this.contextmenu = _.extend(this.contextmenu || {}, options.contextmenu);
                }
                if (this.contextmenu) {
                    treeOptions.plugins = _.union(treeOptions.plugins, ['contextmenu']);
                    treeOptions.contextmenu = this.contextmenu;
                }

                // establish the tree with all configured options
                this.$jstree = this.$el.jstree(treeOptions);
                this.jstree = this.$el.jstree(true);

                if (_.isFunction(this.renameNode)) {
                    this.$jstree.off('dblclick').on('dblclick', '.jstree-anchor', _.bind(this.editNodeName, this));
                }

                // bind all event handlers - for initialization and tree functions
                // the initialization events handler will be unbind after execution
                this.delegateEvents({
                    'loaded.jstree': this.initSelectedData,
                    'redraw.jstree': this.onRedrawNode,
                    'open_node.jstree': this.onOpenNode,
                    'select_node.jstree': this.nodeSelected
                });

                $(document)
                    .on('path:selected.' + this.nodeIdPrefix + 'Tree', _.bind(this.onPathSelected, this))
                    .on('path:inserted.' + this.nodeIdPrefix + 'Tree', _.bind(this.onPathInserted, this))
                    .on('path:changed.' + this.nodeIdPrefix + 'Tree', _.bind(this.onPathChanged, this))
                    .on('path:moved.' + this.nodeIdPrefix + 'Tree', _.bind(this.onPathMoved, this))
                    .on('path:deleted.' + this.nodeIdPrefix + 'Tree', _.bind(this.onPathDeleted, this))
            },

            /**
             * the initial selection event handler called after tree loading
             * selects the node specified by the path in the 'initialSelect' instance
             * variable or in the 'data-selected' DOM attribute af the tree element
             */
            initSelectedData: function () {
                if (!this.initialSelect) {
                    this.initialSelect = this.$el.attr('data-selected');
                }
                if (this.initialSelect) {
                    this.selectNode(this.initialSelect);
                    this.initialSelect = undefined;
                }
                this.undelegate('loaded.jstree');
            },

            // ---------------
            // document events

            onPathSelected: function (event, path) {
                var node = this.getSelectedTreeNode();
                console.log(this.nodeIdPrefix + 'tree.onPathSelected(' + path + '):' + node);
                if (!node || node.original.path != path) {
                    if (!this.busy) {
                        // prevent from endless self activation
                        this.busy = true;
                        try {
                            this.selectNode(path, _.bind(function (path) {
                                var node = this.getSelectedTreeNode();
                                if (!node) {
                                    if (_.isFunction(this.onPathSelectedFailed)) {
                                        this.onPathSelectedFailed(path);
                                    }
                                }
                            }, this));
                        } finally {
                            this.busy = false;
                        }
                    }
                }
            },

            onPathInserted: function (event, parentPath, nodeName) {
                var nodeId = this.nodeId(parentPath);
                console.log(this.nodeIdPrefix + 'tree.onPathInserted(' + parentPath + ',' + nodeName + '):' + nodeId);
                this.refreshNodeById(nodeId);
            },

            onPathChanged: function (event, path) {
                var nodeId = this.nodeId(path);
                console.log(this.nodeIdPrefix + 'tree.onPathChanged(' + path + '):' + nodeId);
                this.refreshNodeById(nodeId);
            },

            onPathMoved: function (event, oldPath, newPath) {
                var oldNode = this.getTreeNodeByPath(oldPath);
                console.log(this.nodeIdPrefix + 'tree.onPathMoved(' + oldPath + ',' + newPath + '):' + oldNode);
                if (oldNode) {
                    var selected = this.getSelectedTreeNode();
                    var restoreSelection = (selected && selected.original.path == oldPath);
                    var oldParentPath = core.getParentPath(oldPath);
                    var parentPath = core.getParentPath(newPath);
                    if (oldParentPath != parentPath) {
                        var oldParentId = this.nodeId(oldParentPath);
                        this.refreshNodeById(oldParentId);
                    }
                    var parentId = this.nodeId(parentPath);
                    var p = this.jstree.get_node(parentId, false);
                    p.state.loaded = false;
                    this.refreshNodeById(parentId, _.bind(function () {
                        if (restoreSelection) {
                            this.selectNode(newPath);
                        }
                    }, this));
                }
            },

            onPathDeleted: function (event, path) {
                var deleted = this.getTreeNodeByPath(path);
                console.log(this.nodeIdPrefix + 'tree.onPathDeleted(' + path + '):' + deleted);
                if (deleted) {
                    var selected = this.getSelectedTreeNode();
                    var nearestFocus = this.findNearestOfDeletion(path);
                    var nearestSelection = undefined;
                    if (selected && selected.original.path == path) {
                        nearestSelection = this.findNearestOfDeletion(path);
                    }
                    this.refreshNodeById(this.getParentNodeId(deleted.id), _.bind(function () {
                        if (nearestSelection) {
                            var path = nearestSelection.original.path;
                            this.selectNode(path);
                        }
                        if (nearestFocus) {
                            var path = nearestFocus.original.path;
                            var $anchor = this.get$TreeNodeAnchor(path);
                            $anchor.focus();
                        }
                    }, this));
                }
            },

            findNearestOfDeletion: function (path) {
                var node = this.getTreeNode(path);
                if (node) {
                    var $nearest = this.jstree.get_next_dom(node, true);
                    if (!$nearest || $nearest.length < 1) {
                        $nearest = this.jstree.get_prev_dom(node, true);
                    }
                    if (!$nearest || $nearest.length < 1) {
                        $nearest = this.jstree.get_prev_dom(node);
                    }
                    if ($nearest && $nearest.length > 0) {
                        var nearest = this.jstree.get_node($nearest[0].id);
                        return nearest;
                    }
                }
                return undefined;
            },

            // ---------------------
            // node ID and data load

            // the default instance prefix,
            // make it unique it if more than one tree is used on one page
            nodeIdPrefix: 'TR_',

            /**
             * Builds a DOM id for an identifier of a node;
             * the identifier can be a node path (string) or an array of names (splitted path)
             */
            nodeId: function (id) {
                // check for the prefix to avoid double encoding
                if (id && (typeof id !== 'string' || id.indexOf(this.nodeIdPrefix) != 0)) {
                    if (_.isArray(id)) {
                        // join an array of names to a path for encoding
                        id = id.join('/');
                    }
                    // use 'base64url' encoded path as id (adoption necessary for jQuery)
                    id = $.base64.encode(id).replace(/=/g, '-').replace(/[/]/g, '_');
                    // add prefix for the current tree instance
                    id = this.nodeIdPrefix + id;
                }
                return id;
            },

            /**
             * loads the data for the node (jstree.core.data function)
             * @param node the 'jstree' node object
             * @param callback the function to assign the data loaded
             */
            nodeData: function (node, callback) {
                // use the URL build by the 'dataUrlForNode' function in the Ajax call
                var url = this.dataUrlForNode(node);
                var tree = this; // for later use via closure
                console.log(this.nodeIdPrefix + 'tree.nodeData(' + node + '): ' + url);
                core.getJson(url, function (result) {
                        // transform all ids (node path) into the tree (jQuery) compatible format
                        result.id = tree.nodeId(result.path);
                        if (result.children) {
                            for (var i = 0; i < result.children.length; i++) {
                                // transform the ids for each child in the children list also
                                result.children[i].id = tree.nodeId(result.children[i].path);
                            }
                        }
                        callback.call(tree.$jstree, result);
                    }
                );
            },

            /**
             * builds the URL to load the data of one node
             */
            dataUrlForNode: function (node) {
                var path;
                if (node.original) {
                    if (node.original.path) {
                        path = node.original.path
                    }
                }
                path = path ? window.core.encodePath(path) : '/';
                if (_.isFunction(this.dataUrlForPath)) {
                    return this.dataUrlForPath(path);
                }
                var params = this.filter && 'default' != this.filter ? '?filter=' + this.filter : '';
                return '/bin/cpm/nodes/node.tree.json' + path + params;
            },

            /**
             * declares the attributes and classes according to the state (JCR state)
             * of the node loaded in the 'original' data object of the node
             */
            refreshNodeStateById: function (id) {
                var node = this.jstree.get_node(id);
                if (node && node.original) {
                    var $node = this.jstree.get_node(id, true);
                    if ($node.length > 0) {
                        this.refreshNodeState($node, node);
                    }
                }
                return node;
            },

            /**
             * declares the attributes and classes according to the state (JCR state)
             * of the node loaded in the 'original' data object of the node
             */
            refreshNodeState: function ($node, node) {
                if (node.original.type) {
                    $node.attr('data-type', node.original.type);
                }
                if (node.original.contentType) {
                    $node.attr('data-content-type', node.original.contentType);
                }
                if (node.original.treeType) {
                    $node.addClass(node.original.treeType);
                }
                if (node.original.jcrState) {
                    var state = node.original.jcrState;
                    if (state.checkedOut && state.isVersionable) {
                        $node.addClass('checked-out');
                    }
                    if (state.locked) {
                        $node.addClass('locked');
                        if (state.lock.isDeep) {
                            $node.addClass('deep-lock');
                        }
                        if (state.lock.isHolder) {
                            $node.addClass('lock-holder');
                        }
                    }
                }
                return node;
            },

            getTreeNodeByPath: function (path) {
                var id = this.nodeId(path);
                var node = this.getTreeNodeById(id);
                return node;
            },

            getTreeNodeById: function (id) {
                return this.jstree.get_node(id);
            },

            // -----------------------
            // 'jstree' event handlers

            editNodeName: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var selection = this.jstree.get_selected();
                if (selection.length > 0) {
                    var nodeId = selection[0];
                    var node = this.jstree.get_node(nodeId);
                    var oldName = node.text;
                    this.jstree.edit(node, null, _.bind(function (node, success, cancelled) {
                        if (success && !cancelled) {
                            var newName = node.text.replace(/^\s+/, '').replace(/\s+$/, '');
                            if (newName != oldName) {
                                this.renameNode(node.original, oldName, newName);
                            }
                        }
                    }, this));
                }
            },

            /**
             * the 'check_callback' is used here as the final drag and drop handler
             * and determines the dragged and the target node and the position in the target node
             * to delegates these to the 'this.dropNode' function which must be provided
             * by the Tree 'superclass' or 'instance' if that extension has
             * the 'dnd' functionality switched on.
             */
            checkCallback: function (op, node, par, pos, more) {
                if (op == 'move_node') {
                    if (node && par) {
                        var dropNode = par.original;
                        var dragNode = node.original;
                        if (dragNode.path != dropNode.path) {
                            var reorder = (dropNode.path == core.getParentPath(dragNode.path));
                            if (!reorder || pos != this.getNodeIndex(node)) {
                                if (_.isFunction(this.dropNode)) {
                                    this.dropNode(dragNode, dropNode, pos, reorder);
                                } else {
                                    core.alert('warning', 'Drag Stop... (no implemented action)',
                                        'dragged: ' + dragNode.path + '<br/>' +
                                        'to target: ' + dropNode.path + " / pos: " + pos +
                                        ' (' + (reorder ? 'reorder' : 'move') + ')');
                                }
                            }
                        }
                    }
                    return false;
                }
                return true;
            },

            /**
             * 'jstree' eventhandler for 'selected'
             * the node selected event handler opens the selected node and
             * calls the 'onNodeSelected' function if declared
             */
            nodeSelected: function (event, data) {
                var id = data.node.id;
                var path = data.node.original.path;
                var $node = this.$('#' + (id ? id : this.nodeId(path)));
                this.jstree.open_node($node);
                this.onNodeSelected(path, data.node, $node);
            },

            onNodeSelected: function (path, node, element) {
                if (!this.busy) {
                    console.log(this.nodeIdPrefix + 'tree.onNodeSelected(' + path + ',' + node + ',' + element + ')');
                    $(document).trigger("path:select", [path, node, element]);
                }
            },

            /**
             * 'jstree' eventhandler for 'redraw'
             * refreshes the node state
             * @param data data.nodes - the list of node ids
             */
            onRedrawNode: function (event, data) {
                for (var i = 0; i < data.nodes.length; i++) {
                    var node = this.refreshNodeStateById(data.nodes[i]);
                }
            },

            /**
             * 'jstree' event handler for 'open_node'
             * refreshes the node state for the node itself and its children
             * @param data data.node - the node (model), see 'jstree'
             */
            onOpenNode: function (event, data) {
                if (data.node) {
                    this.refreshNodeStateById(data.node.id);
                    if (data.node.children) {
                        for (var j = 0; j < data.node.children.length; j++) {
                            this.refreshNodeStateById(data.node.children[j]);
                        }
                    }
                }
            },

            // jstree helpers

            refreshNodeById: function (id, callback) {
                console.log(this.nodeIdPrefix + 'tree.refreshNodeById(' + id + ')');
                if (!id) {
                    var selected = this.jstree.get_selected();
                    if (selected && selected.length > 0) {
                        id = selected[0];
                    }
                }
                if (id) {
                    var selected = this.jstree.get_selected();
                    this.jstree.load_node(id, _.bind(function () {
                        this.jstree.open_node(id, _.bind(function () {
                            if (selected && this.jstree.get_node(selected)) {
                                this.jstree.select_node(selected, true);
                                this.refreshNodeStateById(selected);
                            }
                            if (_.isFunction(callback)) {
                                callback.call(this, selected);
                            }
                        }, this));
                    }, this));
                }
            },

            getParentNodeId: function (mixed) {
                var parentId = undefined;
                if (mixed) {
                    var node = this.jstree.get_node(mixed);
                    if (node && node.original) {
                        var path = node.original.path;
                        path = core.getParentPath(path);
                        parentId = this.nodeId(path);
                    }
                }
                return parentId;
            },

            getNodeIndex: function (mixed) {
                if (mixed) {
                    var node = this.jstree.get_node(mixed);
                    if (node && node.original) {
                        var path = node.original.path;
                        var parentPath = core.getParentPath(path);
                        if (parentPath) {
                            var parentId = this.nodeId(parentPath);
                            var parentNode = this.jstree.get_node(parentId);
                            if (parentNode && parentNode.original) {
                                var children = parentNode.original.children;
                                if (children) {
                                    for (var i = 0; i < children.length; i++) {
                                        if (children[i].path == path) {
                                            return i;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return undefined;
            }
        });

    })(core.components);

})(window.core);
