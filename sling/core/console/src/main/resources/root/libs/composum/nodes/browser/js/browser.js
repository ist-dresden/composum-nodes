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
            if (!browser.current || browser.current.path !== path) {
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
                        $(document).trigger("path:selected", []);
                    }
                }
            }
        };

        browser.refreshCurrentPath = function (path, callback) {
            if (!path && browser.current) {
                path = browser.current.path;
            }
            if (path) {
                core.getJson('/bin/cpm/nodes/node.tree.json' + core.encodePath(path),
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

        browser.Tree = core.console.Tree.extend({

            nodeIdPrefix: 'BT_',

            initialize: function (options) {
                core.console.Tree.prototype.initialize.apply(this, [options]);
            },

            getProfileId: function () {
                return 'browser'
            },

            initializeFilter: function () {
                this.filter = core.console.getProfile().get(this.getProfileId(), 'filter');
            },

            dataUrlForPath: function (path) {
                var params = this.filter && 'default' !== this.filter ? '?filter=' + this.filter : '';
                return '/bin/cpm/nodes/node.tree.json' + path + params;
            }
        });

        browser.tree = core.getView('#browser-tree', browser.Tree);

        browser.TreeActions = core.console.TreeActions.extend({

            initialize: function (options) {
                this.tree = browser.tree;
                core.console.TreeActions.prototype.initialize.apply(this, [options]);
                this.$('button.favorites').on('click', _.bind(this.toggleFavorites, this));
            },

            // @Override
            getCurrent: function () {
                return browser.current;
            },

            // @Override
            getCurrentPath: function () {
                return browser.getCurrentPath();
            },

            // @Override
            setFilter: function (event) {
                event.preventDefault();
                var $link = $(event.currentTarget);
                var filter = $link.text();
                this.tree.setFilter(filter);
                this.setFilterLabel(filter);
                core.console.getProfile().set('browser', 'filter', filter);
            },

            toggleFavorites: function (event) {
                return browser.navigation.toggleFavorites(event);
            }
        });

        browser.treeActions = core.getView('#browser-tree-actions', browser.TreeActions);

        browser.Breadcrumbs = Backbone.View.extend({

            initialize: function (options) {
                this.$toggle = this.$('.breadcrumbs-toggle');
                this.$input = this.$('.path-input-field');
                this.$open = this.$('.open-path');
                this.$list = this.$('.breadcrumbs-list');
                if ('path-input' === core.console.getProfile().get('browser', 'breadcrumbs')) {
                    this.toggle();
                }
                this.$list.find('a').on('click', _.bind(this.pathSelected, this));
                this.$open.click(_.bind(this.open, this));
                this.$input.keypress(_.bind(function (event) {
                    if (event.which === 13) {
                        this.open();
                    }
                }, this));
                this.$toggle.click(_.bind(this.toggle, this));
            },

            toggle: function (event) {
                if (event) {
                    event.preventDefault();
                    core.console.getProfile().set('browser', 'breadcrumbs',
                        this.$el.hasClass('input-path') ? 'breadcrumbs' : 'path-input');
                }
                if (this.$el.hasClass('input-path')) {
                    this.$el.removeClass('input-path');
                    this.$toggle.removeClass('active');
                } else {
                    this.$el.addClass('input-path');
                    this.$toggle.addClass('active');
                }
                return false;
            },

            open: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var path = this.$input.val();
                if (path) {
                    browser.setCurrentPath(path);
                }
                return false;
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
