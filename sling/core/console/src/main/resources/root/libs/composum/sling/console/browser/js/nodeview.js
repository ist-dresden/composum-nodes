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
                this.$parameters = this.$('.detail-toolbar .parameters input');
                this.$('.detail-toolbar .reload').click(_.bind(this.reload, this));
                this.$('.detail-toolbar .open').click(_.bind(this.open, this));
                this.$pathPrefix.val(core.console.getProfile().get(this.displayKey, 'pathPrefix'));
                this.$pathPrefix.on('change.display', _.bind(this.onPrefixChange, this));
                this.$selectors.val(core.console.getProfile().get(this.displayKey, 'selectors'));
                this.$selectors.on('change.display', _.bind(this.onSelectorsChange, this));
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
                    var parts = this.pathPattern.exec(url);
                    if (parts && parts.length > 1) {
                        if (parts[1]) {
                            url = parts[1];
                        } else {
                            url = "";
                        }
                        url += pathPrefix + parts[2];
                    }
                }
                var selectors = this.$selectors.val();
                if (selectors) {
                    var parts = this.urlPattern.exec(url);
                    while (selectors.indexOf('.') === 0) {
                        selectors = selectors.substring(1);
                    }
                    while (selectors.endsWith('.')) {
                        selectors = selectors.substring(0, selectors.length - 1);
                    }
                    url = (parts && parts.length > 1 ? (parts[1] + '.' + selectors + parts[2])
                        : (url + "." + selectors + ".html"));
                }
                var params = this.$parameters.val();
                if (params) {
                    if (params.indexOf('?') === 0) {
                        params = params.substring(1);
                    }
                    url = url + '?' + params;
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
                            browser.tree.selectNode(data.path);
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
                        var mimeType = this.$el.attr('data-type')
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
                ;
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

        browser.PoliciesTable = Backbone.View.extend({

            initialize: function (options) {

                var columns = [{
                    class: 'principal',
                    field: 'principal',
                    title: 'Principal',
                    searchable: true,
                    sortable: true
                }, {
                    class: 'path',
                    field: 'path',
                    title: 'Path',
                    searchable: true,
                    sortable: true
                }, {
                    class: 'rule',
                    field: 'allow',
                    title: 'Rule',
                    searchable: false,
                    sortable: false,
                    width: '50px',
                    formatter: function (value, row, index) {
                        var escaped = _.escape(value);
                        return value ? 'allow' : 'deny';
                    }
                }, {
                    class: 'privileges',
                    field: 'privileges',
                    title: 'Privileges',
                    searchable: true,
                    sortable: false
                }, {
                    class: 'restrictions',
                    field: 'restrictions',
                    title: 'Restrictions',
                    searchable: false,
                    sortable: false
                }];

                if (options.selectable) {
                    columns.unshift({
                        class: 'selection',
                        checkbox: true,
                        sortable: false,
                        width: '50px'
                    });
                }

                this.$el.bootstrapTable(_.extend({

                    search: false,
                    showToggle: false,
                    striped: true,

                    rowStyle: _.bind(function (row, index) {
                        return {
                            classes: (row.allow ? 'allow' : 'deny')
                        };
                    }, this),

                    columns: columns

                }, options));
            },

            getSelections: function () {
                var rows = this.$el.bootstrapTable('getSelections');
                return rows;
            }
        });

        browser.AccessPolicyEntryDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.rule = core.getWidget(this.el, '.rule .radio-group-widget', core.components.RadioGroupWidget);
                this.$principal = this.$('input[name="principal"]');
                this.$privilege = this.$('select[name="privilege"]');
                this.$restriction = this.$('select[name="restrictionKey"]');
                this.$('button.save').click(_.bind(this.saveACL, this));
                this.$privilegeCombobox = core.getWidget(this.el, this.$privilege, core.components.ComboBoxWidget);
                this.$restrictionCombobox = core.getWidget(this.el, this.$restriction, core.components.ComboBoxWidget);
                this.$principal.attr('autocomplete', 'off');
                this.$principal.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        core.getJson('/bin/core/security.principals.json/' + query, function (data) {
                            callback(data);
                        });
                    }
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
                this.rule.setValue('allow');
                this.loadSupportedPrivileges();
                this.loadRestrictionNames();
            },

            loadSupportedPrivileges: function () {
                this.$privilegeCombobox.$el.html('');
                core.getJson("/bin/core/security.supportedPrivileges.json" + browser.getCurrentPath(),
                    _.bind(function (privileges) {
                        for (var i = 0; i < privileges.length; i++) {
                            this.$privilege.append('<option value="' + privileges[i] + '">' + privileges[i] + '</option>');
                        }
                    }, this));
            },

            loadRestrictionNames: function () {
                this.$restrictionCombobox.$el.html('');
                this.$restriction.append('<option value=""></option>');
                core.getJson("/bin/core/security.restrictionNames.json" + browser.getCurrentPath(),
                    _.bind(function (restrictionNames) {
                        for (var i = 0; i < restrictionNames.length; i++) {
                            this.$restriction.append('<option value="' + restrictionNames[i] + '">' + restrictionNames[i] + '</option>');
                        }
                    }, this));
                this.$restriction[0].selectedIndex = -1;
            },

            saveACL: function () {
                var path = browser.getCurrentPath();

                function privilegeValues(arrayOfSelects) {
                    var stringValues = [];
                    for (var i = 0; i < arrayOfSelects.length; i++) {
                        stringValues[i] = $(arrayOfSelects[i]).val();
                    }
                    return stringValues;
                }

                function restrictionValues(arrayOfSelects) {
                    var restrictionStrings = [];
                    for (var i = 0; i < arrayOfSelects.length; i++) {
                        var key = $(arrayOfSelects[i]).val();
                        if (key != '') {
                            var value = $(arrayOfSelects[i]).parent().find('input[name="restrictionValue"]').val();
                            restrictionStrings[i] = key + '=' + value;
                        }
                    }
                    return restrictionStrings;
                }

                var privilegeStrings = privilegeValues($('select[name="privilege"]'));
                var restrictionStrings = restrictionValues($('select[name="restrictionKey"]'));

                core.ajaxPut("/bin/core/security.accessPolicy.json" + path,
                    JSON.stringify({
                        principal: $(".form-control[name='principal']")[0].value,
                        allow: $(".form-control>div.allow input")[0].checked,
                        privileges: privilegeStrings,
                        restrictions: restrictionStrings,
                        path: path
                    }), {
                        dataType: 'json'
                    }, _.bind(function (result) {
                        this.hide();
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on adding access policy entries', result)
                    }, this));
            }

        });

        browser.openAccessPolicyEntryDialog = function (callback) {
            var dialog = core.getView('#access-policy-entry-dialog', browser.AccessPolicyEntryDialog);
            dialog.show(undefined, callback);
        };

        browser.PoliciesTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.verticalSplit = core.getWidget(this.$el,
                    '.split-pane.vertical-split', core.components.VerticalSplitPane);
                this.localTable = core.getWidget(this.$el, '.local-policies > table', browser.PoliciesTable, {
                    selectable: true
                });
                this.effectiveTable = core.getWidget(this.$el, '.effective-policies > table', browser.PoliciesTable);
                this.$('.acl-toolbar .add').click(_.bind(function () {
                    browser.openAccessPolicyEntryDialog(_.bind(this.reload, this));
                }, this));
                this.$('.acl-toolbar .remove').click(_.bind(this.removeSelection, this));
            },

            reload: function () {
                this.loadTableData(this.localTable, 'local');
                this.loadTableData(this.effectiveTable, 'effective');
            },

            loadTableData: function (table, scope) {
                var path = browser.getCurrentPath();
                core.getJson('/bin/core/security.accessPolicies.' + scope + '.json' + path,
                    _.bind(function (result) {
                        table.$el.bootstrapTable('load', result);
                    }, this),
                    _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on loading policies', result);
                    }, this));
            },

            removeSelection: function (event) {
                var path = browser.getCurrentPath();
                var selected = this.localTable.getSelections();
                var entries = [];
                for (var i = 0; i < selected.length; i++) {
                    entries[i] = selected[i];
                }
                if (path && entries) {
                    core.ajaxDelete("/bin/core/security.accessPolicy.json" + path, {
                        data: JSON.stringify(entries),
                        dataType: 'json'
                    }, _.bind(function (result) {
                        this.reload();
                    }, this), _.bind(function (result) {
                        if (result.status < 200 || result.status > 299) {
                            core.alert('danger', 'Error', 'Error on removing access policy entries', result);
                        } else {
                            this.reload();
                        }
                    }, this));
                }
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
                this.$el.resize(_.bind(this.resize, this));
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
