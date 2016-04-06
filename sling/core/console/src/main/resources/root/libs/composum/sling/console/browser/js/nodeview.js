/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.DisplayTab = core.console.DetailTab.extend({

            pathPattern: new RegExp('^(https?://[^/]+)?(/.*)$'),
            urlPattern: new RegExp('^(.*/[^/]+)(\\.[^.]+)$'),

            initialize: function (options) {
                /* get abstract members
                 options: {
                 displayKey: '...',
                 loadContent: function(url) {
                 this.$iframe.attr('src', url);
                 }
                 },*/
                this.displayKey = options.displayKey;
                this.loadContent = options.loadContent;
                /* general initialization */
                this.$mappedButton = this.$('.detail-toolbar .resolver');
                this.$pathPrefix = this.$('.detail-toolbar .prefix input');
                this.$selectors = this.$('.detail-toolbar .selectors input');
                this.$extension = this.$('.detail-toolbar .extension input');
                this.$suffix = this.$('.detail-toolbar .suffix input');
                this.$parameters = this.$('.detail-toolbar .parameters input');
                this.$('.detail-toolbar .reload').click(_.bind(this.reload, this));
                this.$('.detail-toolbar .open').click(_.bind(this.open, this));
                this.$pathPrefix.val(core.console.getProfile().get(this.displayKey, 'pathPrefix'));
                this.$pathPrefix.on('change.display', _.bind(this.onPrefixChange, this));
                this.$selectors.val(core.console.getProfile().get(this.displayKey, 'selectors'));
                this.$selectors.on('change.display', _.bind(this.onSelectorsChange, this));
                this.$extension.val(core.console.getProfile().get(this.displayKey, 'extension'));
                this.$extension.on('change.display', _.bind(this.onExtensionChange, this));
                this.$suffix.val(core.console.getProfile().get(this.displayKey, 'suffix'));
                this.$suffix.on('change.display', _.bind(this.onSuffixChange, this));
                this.$parameters.val(core.console.getProfile().get(this.displayKey, 'parameters'));
                this.$parameters.on('change.display', _.bind(this.onParametersChange, this));
                this.$mappedButton.on('click.display', _.bind(this.toggleMapped, this));
                this.$mappedButton.addClass(core.console.getProfile().get(this.displayKey, 'mapped', true) ? 'on' : 'off');
            },

            onPrefixChange: function (event) {
                var args = this.$pathPrefix.val();
                core.console.getProfile().set(this.displayKey, 'pathPrefix', args);
            },

            onSelectorsChange: function (event) {
                var args = this.$selectors.val();
                core.console.getProfile().set(this.displayKey, 'selectors', args);
            },

            onExtensionChange: function (event) {
                var args = this.$extension.val();
                core.console.getProfile().set(this.displayKey, 'extension', args);
            },

            onSuffixChange: function (event) {
                var args = this.$suffix.val();
                core.console.getProfile().set(this.displayKey, 'suffix', args);
            },

            onParametersChange: function (event) {
                var params = this.$parameters.val();
                core.console.getProfile().set(this.displayKey, 'parameters', params);
            },

            reload: function () {
                this.loadContent(this.getUrl());
            },

            open: function (event) {
                window.open(this.getUrl());
            },

            isMapped: function () {
                return this.$mappedButton.hasClass('on');
            },

            toggleMapped: function (event) {
                event.preventDefault();
                if (this.isMapped()) {
                    this.$mappedButton.removeClass('on');
                    this.$mappedButton.addClass('off');
                } else {
                    this.$mappedButton.removeClass('off');
                    this.$mappedButton.addClass('on');
                }
                core.console.getProfile().set(this.displayKey, 'mapped', this.isMapped());
                this.reload();
                return false;
            },

            getUrl: function () {
                var url = this.$el.attr(this.isMapped() ? 'data-mapped' : 'data-path');
                var pathPrefix = this.$pathPrefix.val();
                if (pathPrefix) {
                    var pathMatch = this.pathPattern.exec(url);
                    if (pathMatch && pathMatch.length > 1) {
                        if (pathMatch[1]) {
                            url = pathMatch[1];
                        } else {
                            url = "";
                        }
                        url += pathPrefix + pathMatch[2];
                    }
                }
                var urlMatch = this.urlPattern.exec(url);
                if (urlMatch) {
                    url = urlMatch[1];
                }
                var selectors = this.$selectors.val();
                if (selectors) {
                    while (selectors.indexOf('.') === 0) {
                        selectors = selectors.substring(1);
                    }
                    while (selectors.endsWith('.')) {
                        selectors = selectors.substring(0, selectors.length - 1);
                    }
                    url += '.' + selectors;
                }
                var extension = this.$extension.val();
                if (extension) {
                    if (extension.indexOf('.') != 0) {
                        extension = '.' + extension;
                    }
                } else {
                    extension = (urlMatch ? urlMatch[2] : ".html");
                }
                url += extension;
                var suffix = this.$suffix.val();
                if (suffix) {
                    if (suffix.indexOf('/') != 0) {
                        suffix = '/' + suffix;
                    }
                    url += suffix;
                }
                var params = this.$parameters.val();
                if (params) {
                    if (params.indexOf('?') === 0) {
                        params = params.substring(1);
                    }
                    url += '?' + params;
                }
                return url;
            }
        });

        browser.HtmlTab = browser.DisplayTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'htmlView',
                    loadContent: function (url) {
                        this.busy = true; // prevent from 'onFrameLoad' after initialization
                        this.$iframe.attr('src', url);
                    }
                });
                browser.DisplayTab.prototype.initialize.apply(this, [options]);
                this.$iframe = this.$('.embedded iframe');
                this.$iframe.load(_.bind(this.onFrameLoad, this));
            },

            onFrameLoad: function (event) {
                if (!this.busy) {
                    this.busy = true;
                    var url = event.currentTarget.contentDocument.URL;
                    core.ajaxGet('/bin/core/node.resolve.json', {
                            data: {
                                url: url
                            }
                        }, _.bind(function (data) {
                            browser.tree.selectNode(data.path, undefined, true);
                        }, this), undefined, _.bind(function (data) {
                            this.busy = false;
                        }, this)
                    );
                } else {
                    this.busy = false;
                }
            }
        });

        browser.ImageTab = browser.DisplayTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'imageView',
                    loadContent: function (url) {
                        this.$image.attr('src', core.getContextUrl(url));
                    }
                });
                browser.DisplayTab.prototype.initialize.apply(this, [options]);
                this.$image = this.$('.image-frame img');
            }
        });

        browser.VideoTab = browser.DisplayTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'videoView',
                    loadContent: function (url) {
                        var mimeType = this.$el.attr('data-type');
                        this.$source.attr('type', mimeType);
                        this.$source.attr('src', core.getContextUrl(url));
                    }
                });
                browser.DisplayTab.prototype.initialize.apply(this, [options]);
                this.$video = this.$('.video-frame video');
                this.$source = this.$video.find('source');
            }
        });

        browser.EditorTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.editor = core.getWidget(this.$el, '.widget.code-editor-widget', core.components.CodeEditorWidget);
                this.$download = this.$('.editor-toolbar .download');
            },

            reload: function () {
                this.$download.attr('href', this.$('.editor-frame .code-editor').attr('data-path'));
            },

            resize: function () {
                this.editor.resize();
            }
        });

        browser.ScriptTab = browser.EditorTab.extend({

            initialize: function (options) {
                this.verticalSplit = core.getWidget(this.$el,
                    '.split-pane.vertical-split', core.components.VerticalSplitPane);
                browser.EditorTab.prototype.initialize.apply(this, [options]);
                this.verticalSplit.setPosition(this.verticalSplit.checkPosition(120));
                this.$logOutput = this.$('.detail-content .log-output');
                this.$execute = this.$('.editor-toolbar .run-script');
                this.$execute.click(_.bind(this.execute, this));
            },

            execute: function (event) {
                this.delay();
                if (this.scriptIsRunning) {
                    this.poll('stopScript', _.bind(this.scriptStopped, this), _.bind(this.scriptError, this));
                } else {
                    this.poll('startScript', _.bind(this.scriptStarted, this), _.bind(this.scriptError, this));
                }
            },

            scriptStarted: function (data) {
                if (!this.scriptIsRunning) {
                    this.scriptIsRunning = true;
                    this.$logOutput.html('');
                    this.$el.removeClass('error');
                    this.$el.addClass('running');
                }
                if (data) {
                    this.logAppend(data);
                }
                this.delay(1000);
            },

            scriptStopped: function (data) {
                this.delay();
                if (data) {
                    this.logAppend(data);
                }
                if (this.scriptIsRunning) {
                    this.$el.removeClass('running');
                    this.scriptIsRunning = false;
                }
            },

            scriptError: function (xhr) {
                this.scriptStopped(xhr.responseText);
            },

            checkScript: function () {
                if (this.scriptIsRunning) {
                    this.poll('checkScript', _.bind(this.onCheck, this), _.bind(this.scriptError, this));
                }
            },

            onCheck: function (data, message, xhr) {
                var status = xhr ? xhr.getResponseHeader('Script-Status') : undefined;
                if (
                    status == 'initialized' ||
                    status == 'starting' ||
                    status == 'running'
                ) {
                    this.scriptStarted(xhr.responseText);

                } else if (
                    status == 'finished' ||
                    status == 'aborted' ||
                    status == 'error' ||
                    status == 'unknown'
                ) {
                    if (status == 'error') {
                        this.$el.addClass('error');
                    }
                    this.scriptStopped(data);

                } else {
                    this.logAppend(data);
                }
                if (this.scriptIsRunning) {
                    this.delay(1000);
                }
            },

            logAppend: function (data) {
                var $scrollPane = this.$logOutput.parent();
                var vheight = $scrollPane.height();
                var height = this.$logOutput[0].scrollHeight;
                var autoscroll = ($scrollPane.scrollTop() > height - vheight - 30);
                this.$logOutput.append(data);
                if (autoscroll) {
                    height = this.$logOutput[0].scrollHeight;
                    $scrollPane.scrollTop(height - vheight);
                }
            },

            poll: function (operation, onSuccess, onError) {
                var path = browser.getCurrentPath();
                core.ajaxGet('/bin/core/node.' + operation + '.groovy' + path, {}, onSuccess, onError);
            },

            delay: function (duration) {
                if (this.timeout) {
                    clearTimeout(this.timeout);
                    this.timeout = undefined;
                }
                if (duration) {
                    this.timeout = setTimeout(_.bind(this.checkScript, this), duration);
                }
            }
        });

        browser.JsonTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$iframe = this.$('.embedded iframe');
                this.binary = core.getWidget(this.$el, '.json-toolbar .binary', core.components.SelectButtonsWidget);
                this.depth = core.getWidget(this.$el, '.json-toolbar .depth', core.components.NumberFieldWidget);
                this.indent = core.getWidget(this.$el, '.json-toolbar .indent', core.components.NumberFieldWidget);
                var profile = core.console.getProfile().get('jsonView', undefined,
                    {binary: 'link', depth: 5, indent: 2});
                this.binary.setValue(profile.binary);
                this.depth.setValue(profile.depth);
                this.indent.setValue(profile.indent);
                this.binary.$el.on('change.json', _.bind(this.remember, this));
                this.depth.$textField.on('change.json', _.bind(this.remember, this));
                this.indent.$textField.on('change.json', _.bind(this.remember, this));
                this.$('.json-toolbar .reload').click(_.bind(this.reload, this));
                this.$download = this.$('.json-toolbar .download');
                this.$('.json-toolbar .upload').click(_.bind(this.upload, this));
            },

            remember: function () {
                var binary = this.binary.getValue();
                var indent = this.indent.getValue();
                var depth = this.depth.getValue();
                var profile = core.console.getProfile().set('jsonView', undefined,
                    {binary: binary, depth: depth, indent: indent});
                this.reload();
            },

            reload: function () {
                this.$download.attr('href', this.getUrl(0, 0, 'base64', true));
                this.$iframe.attr('src', this.getUrl());
            },

            getUrl: function (depth, indent, binary, download) {
                if (depth === undefined) {
                    depth = this.depth.getValue();
                }
                if (indent === undefined) {
                    indent = this.indent.getValue();
                }
                if (binary === undefined) {
                    binary = this.binary.getValue();
                }
                var path = browser.getCurrentPath();
                var url = '/bin/core/node.' + (download ? 'download.' : '') + binary + '.' + depth + '.json' + path;
                if (indent > 0) {
                    url += '?indent=' + indent;
                }
                return core.getContextUrl(url);
            },

            upload: function () {
                var dialog = core.nodes.getUploadNodeDialog();
                dialog.show(_.bind(function () {
                    var currentPath = browser.getCurrentPath();
                    if (currentPath) {
                        var parentPath = core.getParentPath(currentPath);
                        var nodeName = core.getNameFromPath(currentPath);
                        dialog.initDialog(parentPath, nodeName);
                    }
                }, this));
            }
        });


        //
        // detail view (console)
        //

        browser.detailViewTabTypes = [{
            // the 'properties view' from 'properties.js'
            selector: '> .properties',
            tabType: browser.PropertiesTab
        }, {
            selector: '> .display',
            tabType: browser.HtmlTab
        }, {
            selector: '> .image',
            tabType: browser.ImageTab
        }, {
            selector: '> .video',
            tabType: browser.VideoTab
        }, {
            selector: '> .editor',
            tabType: browser.EditorTab
        }, {
            selector: '> .script',
            tabType: browser.ScriptTab
        }, {
            selector: '> .json',
            tabType: browser.JsonTab
        }, {
            selector: '> .acl',
            tabType: browser.PoliciesTab
        }, {
            selector: '> .versions',
            tabType: browser.VersionsTab
        }, {
            // the fallback to the basic implementation as a default rule
            selector: '> div',
            tabType: core.console.DetailTab
        }];

        /**
         * the node view (node detail) which controls the node view tabs
         */
        browser.NodeView = core.console.DetailView.extend({

            getProfileId: function () {
                return 'browser';
            },

            getCurrentPath: function () {
                return browser.current ? browser.current.path : undefined;
            },

            getViewUri: function () {
                return browser.current.viewUrl;
            },

            getTabUri: function (name) {
                return '/bin/browser.tab.' + name + '.html';
            },

            getTabTypes: function () {
                return browser.detailViewTabTypes;
            },

            initialize: function (options) {
                core.console.DetailView.prototype.initialize.apply(this, [options]);
                $(document).off('path:selected.DetailView')
                    .on('path:selected.NodeView', _.bind(this.onPathSelected, this));
                $(document).on('path:changed.NodeView', _.bind(this.onPathChanged, this));
                this.$el.resize(_.bind(this.resize, this));
            },

            onPathSelected: function (event, path) {
                if (path == this.getCurrentPath()) {
                    this.reload();
                }
            },

            onPathChanged: function (event, path) {
                if (path == this.getCurrentPath()) {
                    this.reload();
                }
            },

            /**
             * resize trigger handler
             */
            resize: function () {
                if (this.viewWidget) {
                    if (_.isFunction(this.viewWidget.resize)) {
                        this.viewWidget.resize();
                    }
                }
            },

            /**
             * (re)load the content with the view for the current node ('browser.getCurrentPath()')
             */
            onReload: function () {
                browser.getBreadcrumbs();
                this.favoriteToggle = this.$detailView.find('.favorite-toggle');
                this.checkFavorite();
                this.favoriteToggle.click(_.bind(this.toggleFavorite, this));
            },

            checkFavorite: function () {
                if (browser.favorites.isFavorite(this.path)) {
                    this.favoriteToggle.addClass('active');
                    return true;
                } else {
                    this.favoriteToggle.removeClass('active');
                    return false;
                }
            },

            toggleFavorite: function (event) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                if (this.path) {
                    $(document).trigger("favorite:toggle", [this.path]);
                    this.checkFavorite();
                }
            }
        });

        browser.nodeView = core.getView('#browser-view', browser.NodeView);

    })(core.browser);

})(window.core);
