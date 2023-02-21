/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('core.components');

    (function (components, core) {

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
            'registry': {'icon': 'fa fa-database text-muted'},
            'package': {'icon': 'fa fa-file-archive-o'},
            'resource-package': {'icon': 'fa fa-file-archive-o'},
            'tenant': {'icon': 'fa fa-university text-info'},
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
            'service': {'icon': 'fa fa-cog text-info'},
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
            'file-text-x-log': {'icon': 'fa fa-file-text-o text-info'},
            'resource-text-plain': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-html': {'icon': 'fa fa-globe text-info'},
            'resource-text-html': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-css': {'icon': 'fa fa-file-code-o text-info'},
            'resource-text-css': {'icon': 'fa fa-file-code-o text-muted'},
            'file-javascript': {'icon': 'fa fa-file-code-o text-info'},
            'resource-javascript': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-javascript': {'icon': 'fa fa-file-code-o text-info'},
            'resource-text-javascript': {'icon': 'fa fa-file-code-o text-muted'},
            'file-text-x-java-properties': {'icon': 'fa fa-file-code-o text-info'},
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
            'resource-syntheticresourceproviderresource': {'icon': 'fa fa-code text-muted'},
            'clientlibraryfolder': {'icon': 'fa fa-share-square-o'}
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
                return $node.children('.jstree-anchor');
            },

            /**
             * refreshes the tree view, clears all cached tree structure and reopens the selected node
             */
            refresh: function (callback) {
                var currentPath = this.getSelectedPath();
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.refresh(' + currentPath + ')>>>');
                }
                this.preventFromSelectLock.lock('refresh for ' + currentPath, true);
                this.delegate('refresh.jstree', _.bind(function () {
                    this.undelegate('refresh.jstree');
                    this.preventFromSelectLock.unlock('refresh for ' + currentPath);
                    if (this.log.getLevel() <= log.levels.DEBUG) {
                        this.log.debug(this.nodeIdPrefix + 'tree.refresh(' + currentPath + ').ends.');
                    }
                    if (_.isFunction(callback)) {
                        callback(currentPath);
                    } else {
                        if (currentPath) {
                            this.selectNode(currentPath, undefined, true);
                        }
                    }
                }, this));
                this.jstree.refresh(true, true);
            },

            /** Helper for selectNode: calculates all the parent paths of a path, excl. parents of the rootPath,
             * and gives that to resultCallback. (Callback because that can need an AJAX call.)  */
            parentPathsOfPath: function (path, rootPath, resultCallback) {
                rootPath = rootPath || '/';
                if (path.indexOf(rootPath) === 0) {
                    var names = path.split('/');
                    var parentPath = ''
                    var result = path !== '/' ? ['/'] : []
                    for (var index = 1; index < names.length - 1; index++) {
                        parentPath = parentPath + '/' + names[index];
                        result.push(parentPath);
                    }
                    if (rootPath !== '/') {
                        var rootNames = rootPath.split('/');
                        result = result.slice(rootNames.length-1);
                    }
                    resultCallback(result);
                } else {
                    this.log.warn(this.nodeIdPrefix + 'tree.prefixPathsOfPath(' + path + ') not matching to root: ' + rootPath);
                    resultCallback([]);
                }
            },

            /**
             * selects the node specified by its node path if the node is accepted by the filter
             * opens all nodes up to the target node automatically
             */
            selectNode: function (path, callback, suppressEvent, event) {
                if (path) {
                    if (this.preventFromSelectLock.isLocked()) {
                        window.setTimeout(_.bind(function () {
                            if (this.log.getLevel() <= log.levels.DEBUG) {
                                this.log.debug(this.nodeIdPrefix + 'tree.selectNode(' + path + ').delay...');
                            }
                            this.selectNode(path, callback, suppressEvent, event);
                        }, this), 100);
                    } else {
                        this.preventFromSelectLock.lock(path);
                        this.resetSelection(suppressEvent);
                        if (this.log.getLevel() <= log.levels.DEBUG) {
                            this.log.debug(this.nodeIdPrefix + 'tree.selectNode(' + path + ')>>>');
                        }
                        var tree = this;
                        var rootPath = this.getRootPath();
                        var parentPaths = undefined;
                        this.parentPathsOfPath(path, rootPath, function (result) {
                            parentPaths = result;
                        });
                        var index = 0;
                        if (path.indexOf(rootPath) === 0) {
                            var $node;
                            var exit = _.bind(function () {
                                tree.undelegate('after_open.jstree');
                                this.preventFromSelectLock.unlock();
                                if (_.isFunction(callback)) {
                                    callback(path);
                                }
                                if (this.log.getLevel() <= log.levels.DEBUG) {
                                    this.log.debug(this.nodeIdPrefix + 'tree.selectNode(' + path + ').exit.');
                                }
                            }, this);
                            var drilldown = function () {
                                var id;
                                if (parentPaths == undefined) {
                                    this.log.debug(this.nodeIdPrefix + 'tree.selectNode(' + path + ').parentPathsDelay...');
                                    window.setTimeout(_.bind(drilldown, this), 100);
                                } else if (index < parentPaths.length) {
                                    id = tree.nodeId(parentPaths[index]);
                                    $node = tree.$('#' + id);
                                    index++;
                                    if ($node && $node.length > 0) {
                                        if (tree.jstree.is_open($node)) {
                                            drilldown.apply(this);
                                        } else {
                                            if (!tree.jstree.is_leaf($node)) {
                                                tree.jstree.open_node($node, function (obj, ok) {
                                                    if (!ok) {
                                                        exit();
                                                    }
                                                });
                                            } else {
                                                exit();
                                            }
                                        }
                                    } else {
                                        exit();
                                    }
                                } else {
                                    id = tree.nodeId(path);
                                    $node = tree.$('#' + id);
                                    if ($node && $node.length > 0) {
                                        tree.jstree.select_node($node, suppressEvent, false, event);
                                        this.scrollIntoView($node);
                                    }
                                    exit();
                                }
                            };
                            this.delegate('after_open.jstree', undefined, _.bind(drilldown, this));
                            $node = this.$('#' + this.nodeId(this.getRootPath()));
                            if ($node && $node.length > 0) {
                                if (this.jstree.is_open($node)) {
                                    drilldown.apply(this);
                                } else {
                                    if (!this.jstree.is_leaf($node)) {
                                        this.jstree.open_node($node, function (obj, ok) {
                                            if (!ok) {
                                                exit();
                                            }
                                        });
                                    } else {
                                        exit();
                                    }
                                }
                            } else {
                                exit();
                            }
                        } else {
                            this.log.warn(this.nodeIdPrefix + 'tree.selectNode(' + path + ') not matching to root: ' + rootPath);
                        }
                    }
                } else {
                    this.resetSelection(suppressEvent);
                }
            },

            resetSelection: function (suppressEvent) {
                if (this.getSelectedTreeNode()) {
                    this.jstree.deselect_all(suppressEvent);
                }
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
                if (this.preventFromSelectLock.isLocked()) {
                    window.setTimeout(_.bind(function () {
                        this.setRootPath(rootPath, refresh);
                    }, this), 100);
                } else {
                    if (this.rootPath !== rootPath) {
                        this.rootPath = rootPath;
                        if (refresh !== false) {
                            refresh = true;
                        }
                    }
                    if (refresh) {
                        this.refresh();
                    }
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
                            'name': 'default'
                        },
                        'force_text': true
                    },
                    'types': components.treeTypes
                };

                this.log = options.log || log.getLogger('tree');

                // FIXME NO CHECKIN; for debugging
                this.log.setLevel(log.levels.DEBUG);
                this.preventFromSelectLock.log.setLevel(log.levels.DEBUG);

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
                    .on('path:selected.' + this.nodeIdPrefix + 'tree', _.bind(this.onPathSelected, this))
                    .on('path:inserted.' + this.nodeIdPrefix + 'tree', _.bind(this.onPathInserted, this))
                    .on('path:changed.' + this.nodeIdPrefix + 'tree', _.bind(this.onPathChanged, this))
                    .on('path:moved.' + this.nodeIdPrefix + 'tree', _.bind(this.onPathMoved, this))
                    .on('path:deleted.' + this.nodeIdPrefix + 'tree', _.bind(this.onPathDeleted, this))
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
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.initSelectedData(' + this.initialSelect + ')');
                }
                if (this.initialSelect) {
                    this.selectNode(this.initialSelect);
                    this.initialSelect = undefined;
                }
                this.undelegate('loaded.jstree');
            },

            // ---------------
            // document events

            /**
             * @returns {boolean} 'true', if the path matches to the selected node, 'false' if not (node selection is triggered...)
             */
            onPathSelected: function (event, path) {
                var node = this.getSelectedTreeNode();
                if (!node || node.original.path !== path) {
                    if (this.log.getLevel() <= log.levels.DEBUG) {
                        this.log.debug(this.nodeIdPrefix + 'tree.onPathSelected(' + path + '): '
                            + (node ? JSON.stringify(node.original) : 'undefined'));
                    }
                    this.selectNode(path, _.bind(this.checkSelectedPath, this));
                    return false;
                }
                return true;
            },

            checkSelectedPath: function (path) {
                var node = this.getSelectedTreeNode();
                if (!node) {
                    if (_.isFunction(this.onPathSelectedFailed)) {
                        this.onPathSelectedFailed(path);
                    }
                }
            },

            onPathInserted: function (event, parentPath, nodeName) {
                var nodeId = this.nodeId(parentPath);
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onPathInserted(' + parentPath + ',' + nodeName + ')>>>:' + nodeId);
                }
                this.preventFromSelectLock.lock('pathInserted ' + parentPath, true); // nodes have to be loaded before being selected
                this.ensureNodeExists(parentPath, _.bind(function () {
                    this.refreshNodeById(nodeId,  _.bind(function () {
                        if (this.log.getLevel() <= this.log.levels.DEBUG) {
                            this.log.debug(this.nodeIdPrefix + 'tree.onPathInserted(' + parentPath + ',' + nodeName + ').exit.');
                        }
                        this.preventFromSelectLock.unlock('pathInserted ' + parentPath);
                    }, this));
                }, this));
            },

            /** Makes sure the (new) node at path exists in the tree by calling
             * jstree.load_node on the parents of nodes that weren't reflected in the tree yet.
             * callbackWhenDone is called once loading is finished or immediately, if there wasn't anything to load. */
            ensureNodeExists: function(path, callbackWhenDone) {
                var callbackQueued = false
                if (path && !this.getTreeNode(path)) {
                    var parentPath = core.getParentPath(path)
                    if (parentPath && parentPath !== path) {
                        this.ensureNodeExists(parentPath,
                            _.bind(function () {
                                this.load_node_with_retry(this.nodeId(parentPath), callbackWhenDone);
                            }, this));
                        callbackQueued = true;
                    }

                }
                if (!callbackQueued) {
                    callbackWhenDone();
                }
            },

            /** jstree.load_node seems to throw quite often a "TypeError: Cannot read properties of undefined (reading 'state')" for an unknown reason.
             * As a workaround we retry, which seems to work. TODO find a better solution */
            load_node_with_retry: function (node, callback) {
                try {
                    this.jstree.load_node(node, callback);
                } catch (err1) {
                    this.log.info('First failure on loading ' + node + ' : ' + err1.message);
                    try {
                        this.jstree.load_node(node, callback);
                    } catch (err2) {
                        this.log.warn('Second failure on loading ' + node + ' : ' + err2.message);
                        if (_.isFunction(callbackWhenDone)) {
                            callbackWhenDone(); // give up, but call callback because it resets a lock sometimes
                        }
                    }
                }
            },

            onPathChanged: function (event, path) {
                var nodeId = this.nodeId(path);
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onPathChanged(' + path + '):' + nodeId);
                }
                this.refreshNodeById(nodeId);
            },

            onPathMoved: function (event, oldPath, newPath) {
                var oldNode = this.getTreeNodeByPath(oldPath);
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onPathMoved(' + oldPath + ',' + newPath + '):' + oldNode);
                }
                if (oldNode) {
                    var selected = this.getSelectedTreeNode();
                    var restoreSelection = (selected && selected.original.path === oldPath);
                    var oldParentPath = core.getParentPath(oldPath);
                    var parentPath = core.getParentPath(newPath);
                    if (oldParentPath !== parentPath) {
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

            /** Event handler for path:deleted: newPath optionally determines which path should be displayed afterwards. */
            onPathDeleted: function (event, path, newPath) {
                var deleted = this.getTreeNodeByPath(path);
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.onPathDeleted(' + path + '):' + deleted);
                }
                if (deleted) {
                    var selected = this.getSelectedTreeNode();
                    var nextFocus = this.getTreeNode(newPath) || this.findNearestOfDeletion(path);
                    var nextSelection = undefined;
                    if (selected && selected.original.path === path) {
                        nextSelection = this.getTreeNode(newPath) || this.findNearestOfDeletion(path);
                    }
                    this.refreshNodeById(this.getParentNodeId(deleted.id), _.bind(function () {
                        var path;
                        if (nextSelection) {
                            path = nextSelection.original.path;
                            this.selectNode(path);
                        }
                        if (nextFocus) {
                            path = nextFocus.original.path;
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
                        return this.jstree.get_node($nearest[0].id);
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
                if (id && (typeof id !== 'string' || id.indexOf(this.nodeIdPrefix) !== 0)) {
                    if (_.isArray(id)) {
                        // join an array of names to a path for encoding
                        id = id.join('/');
                    }
                    // use 'base64url' encoded path as id (adoption necessary for jQuery)
                    id = Base64.encode(id).replace(/=/g, '-').replace(/[/]/g, '_');
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
                core.getJson(url, _.bind(function (result) {
                        // transform all ids (node path) into the tree (jQuery) compatible format
                        result.id = tree.nodeId(result.path);
                        if (result.children) {
                            for (var i = 0; i < result.children.length; i++) {
                                // transform the ids for each child in the children list also
                                result.children[i].id = tree.nodeId(result.children[i].path);
                            }
                        }
                        if (this.log.getLevel() <= log.levels.TRACE) {
                            this.log.trace(tree.nodeIdPrefix + 'tree.nodeData(' + url + '): ' + JSON.stringify(result));
                        }
                        try { // TODO; interim 'fix' for an unexpected exception from 'jstree' during initialization
                            callback.call(tree.$jstree, result);
                        } catch (err) {
                            this.log.warn(err.message);
                        }
                    }, this)
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
                path = CPM.core.encodePath(path ? path : this.getRootPath());
                if (_.isFunction(this.dataUrlForPath)) {
                    return this.dataUrlForPath(path);
                }
                var params = this.filter && 'default' !== this.filter ? '?filter=' + this.filter : '';
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
                return this.getTreeNodeById(id);
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
                            if (newName !== oldName) {
                                this.renameNode(node.original, oldName, newName);
                            }
                        }
                    }, this));
                }
            },

            /**
             * the 'check_callback' is used here as the final drag and drop handler (if not in HTML5 mode)
             * and determines the dragged and the target node and the position in the target node
             * to delegates these to the 'this.dropNode' function which must be provided
             * by the Tree 'superclass' or 'instance' if that extension has
             * the 'dnd' functionality switched on.
             */
            checkCallback: function (op, node, par, pos, more) {
                if (op === 'move_node') {
                    if (!this.jstree.settings.dnd.use_html5) {
                        if (node && par && node.original && par.original) {
                            var dropNode = par.original;
                            var dragNode = node.original;
                            if (dragNode.path !== dropNode.path) {
                                var reorder = (dropNode.path === core.getParentPath(dragNode.path));
                                if (!reorder || pos !== this.getNodeIndex(node)) {
                                    if (_.isFunction(this.dropNode)) {
                                        this.dropNode(dragNode, dropNode, pos, reorder);
                                    }
                                }
                            }
                        }
                    }
                    return false;
                }
                return true;
            },

            /**
             * 'jstree' eventhandler for 'selected' ('select_node.jstree')
             * the node selected event handler opens the selected node and
             * calls the 'onNodeSelected' function if declared
             */
            nodeSelected: function (event, data) {
                var id = data.node.id;
                var path = data.node.original.path;
                var $node = this.$('#' + (id ? id : this.nodeId(path)));
                this.jstree.open_node($node);
                this.onNodeSelected(path, data.node, data && data.event || event);
            },

            /**
             * triggers a 'node:selected(path,node)' event to adjust the view to the new selected path
             * @param path
             * @param node
             * @param event the event that triggered this call (mostly to find event loops)
             */
            onNodeSelected: function (path, node, event) {
                this.$el.trigger(core.makeEvent("node:selected", undefined, event), [path, node]);
            },

            /**
             * 'jstree' eventhandler for 'redraw'
             * refreshes the node state
             * @param event
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
             * @param event
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

            /**
             * @param node the node or the 'original' of a node; can be {undefined}
             * @returns the path from a node; can be {undefined}
             */
            getNodePath: function (node) {
                return node ? (node.original ? node.original.path : node.path) : undefined;
            },

            refreshNodeById: function (id, callback) {
                if (this.log.getLevel() <= log.levels.DEBUG) {
                    this.log.debug(this.nodeIdPrefix + 'tree.refreshNodeById(' + id + ')');
                }
                var selected;
                if (!id) {
                    selected = this.jstree.get_selected();
                    if (selected && selected.length > 0) {
                        id = selected[0];
                    }
                }
                if (id) {
                    selected = this.jstree.get_selected();
                    this.load_node_with_retry(id, _.bind(function () {
                        var opened = this.jstree.open_node(id, _.bind(function () {
                            if (selected && this.jstree.get_node(selected)) {
                                this.jstree.select_node(selected, true);
                                this.refreshNodeStateById(selected);
                            }
                            if (_.isFunction(callback)) {
                                callback.call(this, selected);
                            }
                        }, this));
                        if (!opened && _.isFunction(callback)) { // if the node could not be opened, the callback won't be called.
                            callback.call(this, selected);
                        }
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
                    if (node) {
                        var parentId = this.jstree.get_parent(node);
                        if (parentId) {
                            var parentNode = this.jstree.get_node(parentId);
                            if (parentNode) {
                                var children = parentNode.children;
                                if (children) {
                                    for (var i = 0; i < children.length; i++) {
                                        if (children[i] === node.id) {
                                            return i;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return undefined;
            },

            /**
             * @param mixed the parent tree node
             * @param index the index of the child node
             * @returns the node child at the given index of the given parent node
             */
            getNodeOfIndex: function (mixed, index) {
                if (mixed) {
                    var node = this.jstree.get_node(mixed);
                    if (node && node.children) {
                        return this.jstree.get_node(node.children[index]);
                    }
                }
                return undefined;
            },


            /** Lock to try to prevent event races by deferring new selections until last selection was processed. */
            preventFromSelectLock: {
                log : log.getLogger('tree.preventFromSelectLock'),

                /**
                 * Limit time for delaying events to 20 seconds - that should be enough time to sort things out but prevent
                 * occasional endless loops due to weird race conditions.
                 */
                lockDuration : 20*1000,

                /** The time it was locked; falsy if not locked */
                lockedAtTime: 0,

                /** A message for debug logging why it was locked */
                lockmsg: '',

                isLocked: function() {
                    var isLocked = this.lockedAtTime + this.lockDuration > Date.now();
                    if (this.lockedAtTime && !isLocked) {
                        this.log.warn('tree.preventFromSelectLock timing out, breaking the log of ', this.lockmsg);
                        this.lockedAtTime = 0;
                        this.lockmsg = '';
                    }
                    if (isLocked && this.lockmsg) {
                        this.log.debug('tree.preventFromSelectLock is currently locked for ', this.lockmsg);
                    }
                    return isLocked;
                },

                lock: function(lockmsg, force) {
                    if (!force && this.isLocked()) throw new Error('tree.preventFromSelectLock already locked for ' + this.lockmsg);
                    if (force) {
                        this.log.info('tree.preventFromSelectLock is now locked with force unlock for ', lockmsg, ' (was locked for ', this.lockmsg, ')');
                    } else {
                        this.log.debug('tree.preventFromSelectLock is now locked for ', lockmsg);
                    }
                    this.lockedAtTime = Date.now();
                    this.lockmsg = lockmsg;
                },

                unlock: function(unlockmsg) {
                    if (unlockmsg && this.lockmsg !== unlockmsg) {
                        this.log.info('tree.preventFromSelectLock unlocks now for ', this.lockmsg, ' (but was locked for ', unlockmsg, ')');
                    } else {
                        this.log.debug('tree.preventFromSelectLock unlocks now for ', this.lockmsg);
                    }
                    this.lockedAtTime = 0;
                    this.lockmsg = '';
                }

            }

        });

    })(CPM.core.components, CPM.core);

})();
