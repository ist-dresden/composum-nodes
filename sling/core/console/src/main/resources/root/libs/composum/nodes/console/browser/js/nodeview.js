/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        /**
         * the abstract tab to display the current resource by their own view with
         * typical Sling URL variations specified in the views toolbar
         */
        browser.AbstractDisplayTab = core.console.DetailTab.extend({

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
                browser.nodeView.sourceViewTabVisibility();
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

            getSelectors: function () {
                return this.$selectors.val();
            },

            urlHasModifiers: function () {
                return this.isMapped() || this.$pathPrefix.val() || this.getSelectors() || this.$extension.val()
                    || this.$suffix.val() || this.$parameters.val()
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
                var selectors = this.getSelectors();
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
                    if (extension == '-') {
                        extension = '';
                    } else {
                        if (extension.indexOf('.') != 0) {
                            extension = '.' + extension;
                        }
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

        browser.HtmlTab = browser.AbstractDisplayTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'htmlView',
                    loadContent: function (url) {
                        this.busy = true; // prevent from 'onFrameLoad' after initialization
                        this.$iframe.attr('src', url);
                    }
                });
                browser.AbstractDisplayTab.prototype.initialize.apply(this, [options]);
                this.$iframe = this.$('.embedded iframe');
                this.$iframe.on('load.preview', _.bind(this.onFrameLoad, this));
            },

            onFrameLoad: function (event) {
                if (!this.busy) {
                    this.busy = true;
                    var url = event.currentTarget.contentDocument.URL;
                    core.ajaxGet('/bin/cpm/nodes/node.resolve.json', {
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

        /**
         * an abstract display view with 'download' link and upload function for file content update
         */
        browser.AbstractFileTab = browser.AbstractDisplayTab.extend({

            initialize: function (options) {
                browser.AbstractDisplayTab.prototype.initialize.apply(this, [options]);
                this.$image = this.$('.image-frame img');
                this.$('.detail-toolbar .update').click(_.bind(this.upload, this));
                this.$download = this.$('.detail-toolbar .download');
            },

            reload: function () {
                browser.AbstractDisplayTab.prototype.reload.apply(this);
                this.$download.attr('href', core.getContextUrl('/bin/cpm/nodes/node.download.bin' + this.$el.data('path')));
            },

            upload: function () {
                var currentPath = browser.getCurrentPath();
                if (currentPath) {
                    var dialog = core.nodes.getUpdateFileDialog();
                    dialog.show(_.bind(function () {
                        dialog.initDialog(currentPath);
                    }, this));
                }
            }
        });

        browser.ImageTab = browser.AbstractFileTab.extend({

            initialize: function (options) {
                this.isAsset = !!this.$el.data('asset');
                options = _.extend(options, {
                    displayKey: 'imageView',
                    loadContent: function (url) {
                        if (!this.urlHasModifiers() && !this.isAsset) {
                            url = '/bin/cpm/nodes/node.load.bin' + url;
                        }
                        this.$image.attr('src', core.getContextUrl(url));
                    }
                });
                browser.AbstractFileTab.prototype.initialize.apply(this, [options]);
                this.$image = this.$('.image-frame img');
            },

            getSelectors: function () {
                var selectors = this.$selectors.val();
                return this.isAsset ? selectors || 'asset' : selectors;
            }
        });

        browser.VideoTab = browser.AbstractFileTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'videoView',
                    loadContent: function (url) {
                        if (!this.urlHasModifiers()) {
                            url = '/bin/cpm/nodes/node.load.bin' + url;
                        }
                        var mimeType = this.$el.data('type');
                        this.$source.attr('type', mimeType);
                        this.$source.attr('src', core.getContextUrl(url));
                    }
                });
                browser.AbstractFileTab.prototype.initialize.apply(this, [options]);
                this.$video = this.$('.video-frame video');
                this.$source = this.$video.find('source');
            }
        });

        browser.EditorTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.editor = core.getWidget(this.$el, '.widget.code-editor-widget', core.components.CodeEditorWidget);
                this.$('.editor-toolbar .update').click(_.bind(this.upload, this));
                this.$download = this.$('.editor-toolbar .download');
            },

            reload: function () {
                this.$download.attr('href', core.getContextUrl('/bin/cpm/nodes/node.download.bin'
                    + this.$('.editor-frame .code-editor').data('path')));
            },

            resize: function () {
                this.editor.resize();
            },

            upload: function () {
                var currentPath = browser.getCurrentPath();
                if (currentPath) {
                    var dialog = core.nodes.getUpdateFileDialog();
                    dialog.show(_.bind(function () {
                        dialog.initDialog(currentPath);
                    }, this));
                }
            }
        });

        browser.ScriptTab = browser.EditorTab.extend({

            jobTopic: 'com/composum/sling/core/script/GroovyJobExecutor',
            purgeAuditKeep: 6,

            initialize: function (options) {
                this.profile = core.console.getProfile().get('browser', 'scriptView', {
                    vertical: undefined,
                    audit: false
                });
                this.verticalSplit = core.getWidget(this.$el,
                    '.split-pane.vertical-split', core.components.VerticalSplitPane);
                browser.EditorTab.prototype.initialize.apply(this, [options]);
                this.$bottomArea = this.$('.detail-content .bottom-area');
                this.$logOutput = this.$bottomArea.find('.log-output');
                this.$history = this.$bottomArea.find('.history');
                this.$historyList = this.$history.find('.executions');
                this.$history.find('.toolbar .audit-link').click(_.bind(this.selectAuditNode, this));
                this.$history.find('.toolbar .refresh').click(_.bind(this.loadHistory, this));
                this.$history.find('.toolbar .purge').click(_.bind(this.purgeHistory, this));
                this.$history.find('.toolbar .close').click(_.bind(this.toggleHistory, this));
                this.$execute = this.$('.editor-toolbar .run-script');
                this.$execute.click(_.bind(this.execute, this));
                this.$toogleHistory = this.$('.editor-toolbar .history');
                this.$toogleHistory.click(_.bind(this.toggleHistory, this));
                this.verticalSplit.setPosition(this.verticalSplit.checkPosition(this.profile.vertical));
                this.verticalSplit.$el.on('resize.' + this.id, _.bind(this.stateChanged, this));
                if (this.profile.audit) {
                    this.toggleHistory();
                }
            },

            reload: function () {
                browser.EditorTab.prototype.reload.apply(this);
                this.setupStatus();
            },

            execute: function (event) {
                if (this.scriptIsRunning) {
                    this.cancelJob();
                } else {
                    this.scriptStarted(); // set status immediately
                    this.startJob();
                }
            },

            scriptStarted: function (data) {
                if (!this.scriptIsRunning) {
                    this.scriptIsRunning = true;
                    this.$logOutput.text('');
                    this.$el.removeClass('error');
                    this.$el.addClass('running');
                }
                if (data) {
                    this.logAppend(data);
                }
                this.loadHistory();
            },

            scriptStopped: function (data) {
                if (data) {
                    this.logAppend(data);
                }
                if (this.scriptIsRunning) {
                    this.$el.removeClass('running');
                    this.scriptIsRunning = false;
                }
                this.loadHistory();
            },

            scriptError: function (text, xhr) {
                this.$el.addClass('error');
                this.scriptStopped(text + xhr.statusText + (xhr.status ? (' (' + xhr.status + ')') : '') + '\n');
                this.delay(true); // probably one last 'pollOutput' after final status reached
            },

            logAppend: function (data) {
                var $scrollPane = this.$logOutput.parent();
                var vheight = $scrollPane.height();
                var height = this.$logOutput[0].scrollHeight;
                var autoscroll = ($scrollPane.scrollTop() > height - vheight - 30);
                this.$logOutput.text(this.$logOutput.text() + data);
                if (autoscroll) {
                    height = this.$logOutput[0].scrollHeight;
                    $scrollPane.scrollTop(height - vheight);
                }
            },

            setupStatus: function () {
                var path = browser.getCurrentPath();
                core.ajaxGet('/bin/cpm/core/jobcontrol.jobs.ACTIVE.json' + path + '?topic=com/composum/sling/core/script/GroovyJobExecutor', {},
                    _.bind(function (data, msg, xhr) {
                        if (data && data.length > 0) {
                            this.scriptStarted();
                            this.logOffset = 0;
                            this.scriptJob = data[0];
                            this.delay(true);
                        }
                    }, this),
                    _.bind(function (xhr) {
                        this.scriptError('Script status check failed: ', xhr);
                    }, this));
            },

            checkJob: function () {
                // pollOutput on each check even if 'scriptIsRunning' is set to 'false'
                this.pollOutput(_.bind(function () {
                    if (this.scriptIsRunning && this.scriptJob) {
                        core.ajaxGet('/bin/cpm/core/jobcontrol.job.json/' + this.scriptJob['slingevent:eventId'], {},
                            _.bind(function (data, msg, xhr) {
                                this.scriptJob = data;
                                if (this.scriptJob['slingevent:finishedState']) {
                                    switch (this.scriptJob['slingevent:finishedState']) {
                                        case 'SUCCEEDED':
                                            this.scriptStopped('Script finished successfully.\n');
                                            break;
                                        case 'STOPPED':
                                            this.scriptStopped('Script execution stopped.\n');
                                            break;
                                        case 'ERROR':
                                            this.scriptError("Script finished with 'Error': ", {
                                                statusText: this.scriptJob['slingevent:resultMessage']
                                            });
                                            break;
                                        default:
                                            this.scriptStopped("Script execution finished with '" +
                                                this.scriptJob['slingevent:finishedState'] + "'.\n");
                                            break;
                                    }
                                }
                            }, this),
                            _.bind(function (xhr) {
                                this.scriptError('Script status check failed: ', xhr);
                            }, this));
                        this.delay(true);
                    }
                }, this));
            },

            startJob: function () {
                var path = browser.getCurrentPath();
                this.delay(); // cancel all open timeouts
                this.logOffset = 0;
                core.ajaxPost('/bin/cpm/core/jobcontrol.job.json', {
                        'event.job.topic': 'com/composum/sling/core/script/GroovyJobExecutor',
                        'reference': path,
                        '_charset_': 'UTF-8'
                    }, {},
                    _.bind(function (data, msg, xhr) {
                        this.scriptJob = data;
                        this.delay(true);
                    }, this),
                    _.bind(function (xhr) {
                        this.scriptError('Script start failed: ', xhr);
                    }, this));
            },

            cancelJob: function () {
                if (this.scriptJob) {
                    core.ajaxDelete('/bin/cpm/core/jobcontrol.job.json/' + this.scriptJob['slingevent:eventId'], {},
                        _.bind(function (data, msg, xhr) {
                            this.logAppend('Script cancellation requested...\n');
                            this.delay(true);
                        }, this),
                        _.bind(function (xhr) {
                            this.scriptError('Script cancellation failed: ', xhr);
                        }, this));
                }
            },

            pollOutput: function (callback, jobId) {
                if (!jobId) {
                    if (this.scriptJob) {
                        jobId = this.scriptJob['slingevent:eventId'];
                    }
                }
                if (jobId) {
                    core.ajaxGet('/bin/cpm/core/jobcontrol.outfile.txt/' + jobId, {
                            headers: {
                                Range: 'bytes=' + this.logOffset + '-' // get all output from last offset
                            }
                        },
                        _.bind(function (data, msg, xhr) {
                            this.logAppend(data);
                            this.logOffset += parseInt(xhr.getResponseHeader('Content-Length'));
                            if (_.isFunction(callback)) {
                                callback.call(this);
                            }
                        }, this),
                        _.bind(function (xhr) {
                            this.logAppend('Script output retrieval failed: '
                                + xhr.statusText + ' (' + xhr.status + ')\n');
                            if (_.isFunction(callback)) {
                                callback.call(this);
                            }
                        }, this));
                }
            },

            /**
             * Set timeout for the next 'checkJob' if duration is 'true' or given as milliseconds.
             * @param duration 'true' (default slice) or milliseconds; 'undefined' / 'false' to clear all timeouts only
             */
            delay: function (duration) {
                if (this.timeout) {
                    clearTimeout(this.timeout);
                    this.timeout = undefined;
                }
                if (duration) {
                    this.timeout = setTimeout(_.bind(this.checkJob, this),
                        typeof duration === 'boolean' ? 500 : duration);
                }
            },

            toggleHistory: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.$bottomArea.toggleClass('history');
                this.loadHistory();
                this.stateChanged();
            },

            loadHistory: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.$historyList.html('');
                if (this.$bottomArea.hasClass('history')) {
                    var path = browser.getCurrentPath();
                    core.ajaxGet('/bin/cpm/core/jobcontrol.jobs.ALL.json' + path + '?topic=com/composum/sling/core/script/GroovyJobExecutor', {},
                        _.bind(function (data, msg, xhr) {
                            for (var i = 0; i < data.length; i++) {
                                var state = data[i].jobState;
                                this.$historyList.append('<li class="'
                                    + (state ? state.toLowerCase() : 'unknown')
                                    + '"><a href="#" data-id="'
                                    + data[i]['slingevent:eventId']
                                    + '"><span class="time created">'
                                    + data[i]['slingevent:created']
                                    + '</span><span class="state">'
                                    + state
                                    + '</span><span class="time finished">'
                                    + data[i]['slingevent:finishedDate']
                                    + '</span><span class="result">'
                                    + data[i]['slingevent:resultMessage']
                                    + '</span></a></li>');
                            }
                            this.$historyList.find('a').click(_.bind(this.loadLog, this));
                        }, this),
                        _.bind(function (xhr) {
                            this.logAppend('Script history load failed: ' + xhr.statusText);
                        }, this));
                }
            },

            loadLog: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var $link = $(event.currentTarget);
                this.$logOutput.text('');
                this.logOffset = 0;
                this.pollOutput(_.bind(function () {
                    this.logAppend($link.find('.result').text());
                }, this), $link.data('id'));
            },

            purgeHistory: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var dialog = core.console.getPurgeAuditDialog();
                dialog.show(_.bind(function () {
                    dialog.initDialog(this.jobTopic,
                        browser.getCurrentPath(),
                        this.purgeAuditKeep);
                }, this), _.bind(function () {
                    this.loadHistory();
                }, this));
            },

            selectAuditNode: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var $link = $(event.currentTarget);
                var path = '/var/audit/jobs/com.composum.sling.core.script.GroovyJobExecutor' + $link.data('path');
                $(document).trigger('path:select', [path]);
            },

            stateChanged: function () {
                var last = _.clone(this.profile);
                this.profile.audit = this.$bottomArea.hasClass('history');
                this.profile.vertical = this.verticalSplit.getPosition();
                if (!_.isEqual(last, this.profile)) {
                    core.console.getProfile().set('browser', 'scriptView', this.profile);
                    this.verticalSplit.stateChanged();
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
                this.$('.json-toolbar .menu a').click(_.bind(this.tabSelected, this));
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
                browser.nodeView.sourceViewTabVisibility('json');
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
                var url = '/bin/cpm/nodes/node.' + (download ? 'download.' : '') + binary + '.' + depth + '.json' + path;
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
            },

            tabSelected: function (event) {
                browser.nodeView.tabSelected(event);
            }
        });


        browser.XmlTab = browser.AbstractDisplayTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'xmlView'
                });
                browser.AbstractDisplayTab.prototype.initialize.apply(this, [options]);
                this.$iframe = this.$('.embedded iframe');
                this.$('.xml-toolbar .reload').click(_.bind(this.reload, this));
                this.$('.xml-toolbar .copy').click(_.bind(this.copyToClipboard, this));
                this.$download = this.$('.xml-toolbar .download');
                this.$zip = this.$('.xml-toolbar .zip');
                this.$pkg = this.$('.xml-toolbar .pkg');
                this.$('.xml-toolbar .menu a').click(_.bind(this.tabSelected, this));
            },

            reload: function () {
                this.$iframe.attr('src', this.getUrl());
                this.$download.attr('href', this.getUrl('xml'));
                this.$zip.attr('href', this.getUrl('zip'));
                this.$pkg.attr('href', this.getUrl('pkg'));
                browser.nodeView.sourceViewTabVisibility('xml');
            },

            getUrl: function (type) {
                var type = type || "xml";
                var path = browser.getCurrentPath();
                var url = '/bin/cpm/nodes/source.' + type + path;
                return core.getContextUrl(url);
            },

            copyToClipboard: function () {
                var xmlContentDocument = document.querySelector(".xml .detail-content iframe").contentDocument;
                xmlContentDocument.designMode = "on";
                xmlContentDocument.execCommand("selectAll", false, null);
                xmlContentDocument.execCommand("copy", false, null);
                xmlContentDocument.designMode = "off";
            },

            tabSelected: function (event) {
                browser.nodeView.tabSelected(event);
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
            selector: '> .xml',
            tabType: browser.XmlTab
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
            },

            /**
             * Shows only the selected tab from the source view tabs.
             */
            sourceViewTabVisibility: function (tab) {
                var shownTab = tab || core.console.getProfile().get('browser', 'sourceview');
                if (shownTab) {
                    core.console.getProfile().set('browser', 'sourceview', shownTab);
                    this.$detailView.find('.node-tabs .source').addClass('hidden');
                    this.$detailView.find('.node-tabs .source.' + shownTab).removeClass('hidden');
                }
            }

        });

        browser.nodeView = core.getView('#browser-view', browser.NodeView);

    })(core.browser);

})(window.core);
