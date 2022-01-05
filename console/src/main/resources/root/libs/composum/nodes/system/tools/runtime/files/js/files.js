(function () {
    'use strict';
    CPM.namespace('nodes.system.files');

    (function (files, core) {

        files.profileId = 'system-files';
        files.servlet = '/bin/cpm/system/file';
        files.component = '/libs/composum/nodes/system/content/runtime/files';

        files.Tree = core.components.Tree.extend({

            nodeIdPrefix: 'RF_',

            getProfileId: function () {
                return files.profileId
            },

            initialize: function (options) {
                var id = this.nodeIdPrefix + 'Tree';
                this.initialSelect = core.console.getProfile().get(this.getProfileId(), 'current', "/");
                core.components.Tree.prototype.initialize.apply(this, [options]);
            },

            dataUrlForPath: function (path) {
                return files.servlet + '.tree.json' + core.encodePath(path);
            },

            onNodeSelected: function (path, node) {
                files.panel.fileSelected(node.original);
            }
        });

        files.Actions = Backbone.View.extend({
            initialize: function (options) {
                this.panel = options.panel;
                this.$tail = this.$('.btn.tail');
                this.$scroll = this.$('.btn.scroll');
                this.$wrap = this.$('.btn.wrap');
                this.$limit = this.$('.limit input');
                this.$filter = this.$('.filter input');
                this.$download = this.$('.btn.download');
                this.$reload = this.$('.btn.reload');
                this.$open = this.$('.btn.open');
                this.$limit.val(core.console.getProfile().get(files.profileId, 'limit', undefined));
                this.$filter.val(core.console.getProfile().get(files.profileId, 'filter', undefined));
                this.$limit.on('change', _.bind(function () {
                    core.console.getProfile().set(files.profileId, 'limit', this.$limit.val());
                    this.panel.view.reload();
                }, this));
                this.$filter.on('change', _.bind(function () {
                    core.console.getProfile().set(files.profileId, 'filter', this.$filter.val());
                    this.panel.view.reload();
                }, this));
                this.$reload.click(_.bind(function () {
                    this.panel.view.reload();
                }, this));
                this.$tail.click(_.bind(function () {
                    this.panel.view.toggleTail();
                    this.refreshStatus();
                }, this));
                this.$scroll.click(_.bind(function () {
                    this.panel.view.toggleScroll();
                    this.refreshStatus();
                }, this));
                this.$wrap.click(_.bind(function () {
                    this.panel.toggleWrap();
                    this.refreshStatus();
                }, this));
            },

            fileSelected: function (data) {
                this.$download.attr('href', data.uri ? data.uri : '#');
                if (data.isText && data.path) {
                    this.$open.attr('href', core.getContextUrl(files.component + '.view.html'
                        + core.encodePath(data.path)));
                    this.$open.removeClass('disabled')
                } else {
                    this.$open.attr('href', '#');
                    this.$open.addClass('disabled')
                }
            },

            refreshStatus: function () {
                this.setActive(this.$tail, this.panel.view.tail);
                this.setActive(this.$scroll, this.panel.view.scroll);
                this.setActive(this.$wrap, this.panel.linewrap);
            },

            setActive($el, active) {
                if (active) {
                    $el.addClass('active');
                } else {
                    $el.removeClass('active');
                }
            },

            getLimit: function () {
                return this.$limit.val() || '1000';
            },

            getFilter: function () {
                return this.$filter.val() || '';
            }
        });

        files.View = Backbone.View.extend({

            initialize: function (options) {
                this.panel = options.panel;
                this.$content = this.$('.tools-runtime-files_view > div');
                this.tail = (core.console.getProfile().get(files.profileId, 'tail', false));
                this.scroll = (core.console.getProfile().get(files.profileId, 'scroll', false));
                this.$el.scroll(_.bind(function (event) {
                    if (!this.loading && this.scrollTop && this.scroll && this.file && this.file.isLog) {
                        if (this.scrollTop - this.el.scrollTop > 20) {
                            this.toggleScroll(false);
                            this.panel.actions.refreshStatus();
                        }
                    }
                    this.scrollTop = this.el.scrollTop;
                }, this));
            },

            fileSelected: function (data) {
                this.file = data;
                this.reload();
            },

            toggleTail: function (tail) {
                this.tail = tail !== undefined ? tail : !this.tail;
                this.scroll = scroll !== undefined ? scroll : !this.scroll;
                core.console.getProfile().set(files.profileId, 'tail', this.tail);
                if (this.tail) {
                    this.toggleScroll(true);
                    this.doTail();
                }
            },

            toggleScroll: function (scroll) {
                this.scroll = scroll !== undefined ? scroll : !this.scroll;
                core.console.getProfile().set(files.profileId, 'scroll', this.tail);
                if (this.scroll) {
                    this.scrollToEnd();
                }
            },

            scrollToEnd: function () {
                if (this.scroll) {
                    this.el.scrollTop = this.el.scrollHeight;
                }
            },

            doTail: function () {
                if (this.tail && this.file && this.file.isLog) {
                    var url = encodeURI(files.servlet + '.tail.txt') + core.encodePath(this.file.path)
                        + '?filter=' + encodeURIComponent(this.panel.actions.getFilter());
                    core.ajaxGet(url, {}, _.bind(function (content) {
                        this.$content.addClass(this.file.type + ' text ' + (this.file.isLog ? 'log' : ''));
                        this.$content.append(content);
                        this.scrollToEnd();
                        window.setTimeout(_.bind(this.doTail, this), 500);
                    }, this));
                }
            },

            reload: function () {
                this.$content.removeClass();
                this.$content.text('');
                if (this.file) {
                    if (this.file.isText) {
                        this.loading = true;
                        this.$el.addClass('loading');
                        var url = encodeURI(files.servlet + '.tail.0.' + this.panel.actions.getLimit() + '.txt')
                            + core.encodePath(this.file.path)
                            + '?filter=' + encodeURIComponent(this.panel.actions.getFilter());
                        core.ajaxGet(url, {}, _.bind(function (content) {
                            this.$content.addClass(this.file.type + ' text ' + (this.file.isLog ? 'log' : ''));
                            this.$content.text(content);
                            this.scrollToEnd();
                        }, this), undefined, _.bind(function () {
                            this.$el.removeClass('loading');
                            this.loading = false;
                        }, this));
                    } else {
                        this.$content.addClass(this.file.type);
                    }
                }
            }
        });

        files.Panel = Backbone.View.extend({

            initialize: function () {
                this.$content = this.$('.content-wrapper');
                this.$title = this.$content.find('.tools-runtime-files_header .title');
                this.tree = core.getWidget(this.$el, '.tools-runtime-files_tree', files.Tree);
                if (!this.tree) {
                    var suffix = this.$content.data('suffix');
                    var url = encodeURI(files.servlet + '.tree.json' + (suffix ? suffix : '/'));
                    core.getJson(url, _.bind(function (data) {
                        this.file = data;
                        var dot = data.name.indexOf('.');
                        files.id += '-' + (dot > 0 ? data.name.substring(0, dot) : data.name);
                        this.initViews();
                        this.fileSelected(this.file);
                    }, this));
                } else {
                    this.initViews();
                }
            },

            initViews: function () {
                this.view = core.getWidget(this.$el, '.tools-runtime-files_view-wrapper',
                    files.View, {panel: this});
                this.actions = core.getWidget(this.$el, '.tools-runtime-files_view-toolbar',
                    files.Actions, {panel: this});
                this.toggleWrap(core.console.getProfile().get(files.profileId, 'linewrap', false));
                this.actions.refreshStatus();
            },

            fileSelected: function (data) {
                this.$title.text(data.path && data.path !== '/' ? data.path : 'Runtime Files');
                core.console.getProfile().set(files.profileId, 'current', data.path);
                this.actions.fileSelected(data);
                this.view.fileSelected(data);
                this.view.doTail();
            },

            toggleWrap: function (wrap) {
                this.linewrap = wrap === undefined ? !this.linewrap : wrap;
                core.console.getProfile().set(files.profileId, 'linewrap', this.linewrap);
                this.view.loading = true;
                if (this.linewrap) {
                    this.$el.addClass('linewrap');
                } else {
                    this.$el.removeClass('linewrap');
                }
                this.view.scrollToEnd();
                this.view.loading = false;
            }
        });

        files.panel = core.getView('#tools-runtime-files', files.Panel);

    })(CPM.nodes.system.files, CPM.core);
})();
