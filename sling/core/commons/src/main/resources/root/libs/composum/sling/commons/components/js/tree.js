/**
 *
 *
 */
(function(core) {
    'use strict';

    core.components = core.components || {};

(function(components) {

    /**
     * the node type declarations for the tree (defines the icon classes of the nodes)
     */
    components.treeTypes = {
        'default': { 'icon': 'fa fa-cube text-muted' },
        'summary': { 'icon': 'fa fa-hand-o-right text-info' },
        'root': { 'icon': 'fa fa-sitemap text-muted' },
        'system': { 'icon': 'fa fa-cogs text-muted' },
        'activities': { 'icon': 'fa fa-signal text-muted' },
        'nodetypes': { 'icon': 'fa fa-tags text-muted' },
        'nodetype': { 'icon': 'fa fa-tag text-muted' },
        'versionstorage': { 'icon': 'fa fa-history text-muted' },
        'folder': { 'icon': 'fa fa-folder-o' },
        'resource-folder': { 'icon': 'fa fa-folder-o' },
        'orderedfolder': { 'icon': 'fa fa-folder text-muted' },
        'package': { 'icon': 'fa fa-file-archive-o' },
        'resource-package': { 'icon': 'fa fa-file-archive-o' },
        'component': { 'icon': 'fa fa-puzzle-piece text-info' },
        'page': { 'icon': 'fa fa-globe text-info' },
        'pagecontent': { 'icon': 'fa fa-ellipsis-h text-muted' },
        'page-designer': { 'icon': 'fa fa-cube text-info' },
        'resource-designer': { 'icon': 'fa fa-file-code-o text-muted' },
        'resource-redirect': { 'icon': 'fa fa-share text-info' },
        'resource-parsys': { 'icon': 'fa fa-ellipsis-v text-muted' },
        'resource-console': { 'icon': 'fa fa-laptop text-muted' },
        'resource-pckgmgr': { 'icon': 'fa fa-laptop text-muted' },
        'resource-path': { 'icon': 'fa fa-bookmark-o text-muted' },
        'resource-resources': { 'icon': 'fa fa-filter text-muted' },
        'resource-strings': { 'icon': 'fa fa-filter text-muted' },
        'resource-felix': { 'icon': 'fa fa-cog text-muted' },
        'resource-guide': { 'icon': 'fa fa-book text-muted' },
        'resource-servlet': { 'icon': 'fa fa-cog text-muted' },
        'acl': { 'icon': 'fa fa-key text-muted' },
        'authorizablefolder': { 'icon': 'fa fa-diamond text-muted' },
        'group': { 'icon': 'fa fa-group text-muted' },
        'user': { 'icon': 'fa fa-user' },
        'linkedfile': { 'icon': 'fa fa-link text-muted' },
        'file': { 'icon': 'fa fa-file-o' },
        'resource': { 'icon': 'fa fa-file-o' },
        'resource-file': { 'icon': 'fa fa-file-o text-muted' },
        'file-image': { 'icon': 'fa fa-file-image-o text-info' },
        'resource-image': { 'icon': 'fa fa-file-image-o text-muted' },
        'file-text': { 'icon': 'fa fa-file-text-o text-info' },
        'resource-text': { 'icon': 'fa fa-file-text-o text-muted' },
        'file-text-plain': { 'icon': 'fa fa-file-text-o text-info' },
        'resource-text-plain': { 'icon': 'fa fa-file-code-o text-muted' },
        'file-text-html': { 'icon': 'fa fa-globe text-info' },
        'resource-text-html': { 'icon': 'fa fa-file-code-o text-muted' },
        'file-text-css': { 'icon': 'fa fa-file-code-o text-info' },
        'resource-text-css': { 'icon': 'fa fa-file-code-o text-muted' },
        'file-javascript': { 'icon': 'fa fa-file-code-o text-info' },
        'resource-javascript': { 'icon': 'fa fa-file-code-o text-muted' },
        'file-text-javascript': { 'icon': 'fa fa-file-code-o text-info' },
        'resource-text-javascript': { 'icon': 'fa fa-file-code-o text-muted' },
        'file-text-x-java-source': { 'icon': 'fa fa-file-code-o text-info' },
        'resource-text-x-java-source': { 'icon': 'fa fa-file-code-o text-muted' },
        'file-octet-stream': { 'icon': 'fa fa-file-code-o text-info' },
        'resource-octet-stream': { 'icon': 'fa fa-file-code-o text-muted' },
        'file-pdf': { 'icon': 'fa fa-file-pdf-o text-info' },
        'resource-pdf': { 'icon': 'fa fa-file-pdf-o text-muted' },
        'file-zip': { 'icon': 'fa fa-file-archive-o text-info' },
        'resource-zip': { 'icon': 'fa fa-file-archive-o text-muted' },
        'file-java-archive': { 'icon': 'fa fa-file-archive-o text-info' },
        'resource-java-archive': { 'icon': 'fa fa-file-archive-o text-muted' },
        'asset': { 'icon': 'fa fa-file-o text-info' },
        'assetcontent': { 'icon': 'fa fa-file-o text-muted' },
        'file-binary': { 'icon': 'fa fa-file-o text-info' },
        'resource-binary': { 'icon': 'fa fa-file-o text-muted' },
        'resource-syntheticresourceproviderresource': { 'icon': 'fa fa-code text-muted' }
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
     *            return '/bin/core/node.page.title.json' + path;
     *        },
     *
     *        onNodeSelected: function(path) {
     *            alert('onNodeSelected: ' + path);
     *        }
     *    });
     */
    components.Tree = Backbone.View.extend({

        /**
         * sets the filter key for the tree; the key is the 'name' in the filter OSGi configuration)
         */
        setFilter: function(filter, refresh) {
            this.filter = filter;
            if (refresh === undefined || refresh) {
                this.refresh();
            }
        },

        /**
         * returns the model data of the selected node; 'undefined' if no node is selected
         */
        current: function() {
            var selected = this.$el.jstree('get_selected', true);
            return selected && selected.length > 0 ? selected[0].original : undefined;
        },

        /**
         * refreshes the tree view, clears all cached tree structure and reopens the selected node
         */
        refresh: function() {
            var current = this.current();
            this.delegate('refresh.jstree', _.bind(function(){
                if (current) {
                    this.selectNode(current.path);
                }
                this.undelegate ('refresh.jstree');
            }, this));
            this.$el.jstree('refresh', true, true);
        },

        /**
         * selects the node specified by its node path if the node is accepted by the filter
         * opens all nodes up to the target node automatically
         */
        selectNode: function(path) {
            this.resetSelection();
            if (path) {
                var names = $.isArray(path) ? path : path.split('/');
                var index = 1;
                var $node;
                var drilldown = function () {
                    if (index < names.length-1) {
                        var id = this.nodeId(_.first(names,index+1));
                        $node = this.$('#' + id);
                        index++;
                        if ($node) {
                            if (this.$el.jstree('is_open', $node)) {
                                drilldown.apply(this);
                            } else {
                                if (!this.$el.jstree('is_leaf', $node)) {
                                    this.$el.jstree('open_node', $node);
                                } else {
                                    this.undelegate ('after_open.jstree');
                                }
                            }
                        } else {
                            this.undelegate ('after_open.jstree');
                        }
                    } else {
                        var id = this.nodeId(_.first(names,names.length));
                        $node = this.$('#' + id);
                        if ($node) {
                            this.$el.jstree('select_node', $node);
                            this.scrollIntoView($node);
                        }
                        this.undelegate ('after_open.jstree');
                    }
                };
                this.delegate('after_open.jstree', undefined, _.bind(drilldown, this));
                $node = this.$('#' + this.nodeId('/'));
                if ($node) {
                    if (this.$el.jstree('is_open', $node)) {
                        drilldown.apply(this);
                    } else {
                        if (!this.$el.jstree('is_leaf', $node)) {
                            this.$el.jstree('open_node', $node);
                        } else {
                            this.undelegate ('after_open.jstree');
                        }
                    }
                }
            }
        },

        /**
         * scrolls the trees viewport that the specified node (jQuery element of the node)
         * is visible within the viewport (scrolls if necessary - node not visible - only)
         */
        scrollIntoView: function($node) {
            var $tree = this.$el.jstree('get_container');
            var $panel = $tree.closest('.tree-panel');
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

        resetSelection: function() {
            this.$el.jstree('deselect_all');
        },

        reset: function() {
            this.resetSelection();
        },

        initialize: function(options) {

            var treeOptions = {

                'plugins': [
                    'types',
                    'unique',
                    'wholerow'
                ],
                'core': {
                    'animation': false,
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
                this.dragAndDrop = _.extend (this.dragAndDrop || {}, options.dragAndDrop);
            }
            if (this.dragAndDrop) {
                treeOptions.plugins = _.union(treeOptions.plugins, ['dnd']);
                treeOptions.dnd = this.dragAndDrop;
                $(document).on('dnd_stop.vakata', _.bind (this.dragStop, this));
            }

            //  extend initialization to set up a context menu if configured in the options
            if (options.contextmenu) {
                this.contextmenu = _.extend (this.contextmenu || {}, options.contextmenu);
            }
            if (this.contextmenu) {
                treeOptions.plugins = _.union(treeOptions.plugins, ['contextmenu']);
                treeOptions.contextmenu = this.contextmenu;
            }

            // establish the tree with all configured options
            this.$el.jstree(treeOptions);

            // bind all event handlers - for initialization and tree functions
            // the initialization events handler will be unbind after execution
            this.delegateEvents({
                'loaded.jstree': this.initSelectedData,
                'redraw.jstree': this.onRedrawNode,
                'open_node.jstree': this.onOpenNode,
                'select_node.jstree': this.nodeSelected
            });
        },

        /**
         * the initial selection event handler called after tree loading
         * selects the node specified by the path in the 'initialSelect' instance
         * variable or in the 'data-selected' DOM attribute af the tree element
         */
        initSelectedData: function() {
            if (!this.initialSelect) {
                this.initialSelect = this.$el.attr('data-selected');
            }
            if (this.initialSelect) {
                this.selectNode(this.initialSelect);
                if (_.isFunction (this.onInitialSelect)) {
                    this.onInitialSelect(this.initialSelect);
                }
                this.initialSelect = undefined;
            }
            this.undelegate('loaded.jstree');
        },

        // the default instance prefix,
        // make it unique it if more than one tree is used on one page
        nodeIdPrefix: 'TR_',

        /**
         * Builds a DOM id for an identifier of a node;
         * the identifier can be a node path (string) or an array of names (splitted path)
         */
        nodeId: function(id) {
            // check for the prefix to avoid double encoding
            if (id && (typeof id !== 'string' || id.indexOf(this.nodeIdPrefix) != 0)) {
                if (_.isArray(id)) {
                    // join an array of names to a path for encoding
                    id = id.join('/');
                }
                // use 'base64url' encoded path as id (adoption necessary for jQuery)
                id = $.base64.encode(id).replace(/=/g,'-').replace(/[/]/g,'_');
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
        nodeData: function(node, callback) {
            // use the URL build by the 'dataUrlForNode' function in th Ajax call
            var url = this.dataUrlForNode (node);
            var tree = this; // for later use via closure
            $.ajax({
                type: 'GET',
                url: url,
                dataType: 'json',
                success: function (result) {
                    // transform all ids (node path) into the tree (jQuery) compatible format
                    result.id = tree.nodeId(result.id);
                    if (result.children) {
                        for (var i=0; i < result.children.length; i++) {
                            // transform the ids for each child in the children list also
                            result.children[i].id = tree.nodeId(result.children[i].id);
                        }
                    }
                    callback.call (this, result);
                }
            });
        },

        /**
         * builds the URL to load the data of one node
         */
        dataUrlForNode: function(node) {
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
            return '/bin/core/node.tree.json' + path + params;
        },

        /**
         * the node selected event handler opens the selected node and
         * calls the 'onNodeSelected' function if declared
         */
        nodeSelected: function(event, data) {
            var id = data.node.id;
            var path = data.node.original.path;
            var $node = this.$('#' + (id ? id : this.nodeId(path)));
            this.$el.jstree('open_node', $node);
            if (_.isFunction(this.onNodeSelected)) {
                this.onNodeSelected(path, data.node, $node);
            }
        },

        /**
         * the final drag and drop handler determines the dragged and the target node
         * an delegates these to the 'this.dropNode' function which must be provided
         * by the Tree 'superclass' or 'instance' if this extension has
         * the 'dnd' functionality switched on.
         */
        dragStop: function(event, data) {
            var dragId = data.element.id;
            var dragTreeObj =  this.$el.jstree('get_node', dragId);
            var dropTarget = data.event.target;
            var $dropNode = $(dropTarget).closest('li.jstree-node');
            var dropId = $dropNode.attr('id');
            if (dragTreeObj && dropId) {
                var dropTreeObj = this.$el.jstree('get_node', dropId);
                if (dropTreeObj) {
                    var dropNode = dropTreeObj.original;
                    var dragNode = dragTreeObj.original;
                    if (_.isFunction (this.dropNode)) {
                        this.dropNode (dragNode, dropNode);
                    } else {
                        core.alert ('warning', 'Drag Stop... (no implemented action)',
                                    'dragged: ' + dragNode.path + '<br/>' +
                                    'to target: ' + dropNode.path);
                    }
                }
            }
        },

        /**
         * 'jstree' eventhandler for 'redraw'
         * refreshes the node state
         * @param data data.nodes - the list of node ids
         */
        onRedrawNode: function(event, data) {
            for (var i=0; i < data.nodes.length; i++) {
                var node = this.refreshNodeStateById(data.nodes[i]);
            }
        },

        /**
         * 'jstree' event handler for 'open_node'
         * refreshes the node state for the node itself and its children
         * @param data data.node - the node (model), see 'jstree'
         */
        onOpenNode: function(event, data) {
            if(data.node) {
                this.refreshNodeStateById(data.node.id);
                if (data.node.children) {
                    for (var j=0; j < data.node.children.length; j++) {
                        this.refreshNodeStateById(data.node.children[j]);
                    }
                }
            }
        },

        /**
         * declares the attributes and classes according to the state (JCR state)
         * of the node loaded in the 'original' data object of the node
         */
        refreshNodeStateById: function(id) {
            var node = this.$el.jstree('get_node', id);
            if (node && node.original) {
                var $node = this.$el.jstree('get_node', id, true);
                if ($node.length > 0) {
                    this.refreshNodeState ($node, node);
                }
            }
            return node;
        },

        /**
         * declares the attributes and classes according to the state (JCR state)
         * of the node loaded in the 'original' data object of the node
         */
        refreshNodeState: function($node, node) {
            if (node.original.type) {
                $node.attr ('data-type', node.original.type);
            }
            if (node.original.contentType) {
                $node.attr ('data-content-type', node.original.contentType);
            }
            if (node.original.treeType) {
                $node.addClass (node.original.treeType);
            }
            if (node.original.jcrState) {
                var state = node.original.jcrState;
                if (state.checkedOut && state.isVersionable) {
                    $node.addClass ('checked-out');
                }
                if (state.locked) {
                    $node.addClass ('locked');
                    if (state.lock.isDeep) {
                        $node.addClass ('deep-lock');
                    }
                    if (state.lock.isHolder) {
                        $node.addClass ('lock-holder');
                    }
                }
            }
            return node;
        }
    });

})(core.components);

})(window.core);
