/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('console');
    core.console = CPM.console; // core.console for compatibility ... @deprecated

    (function (console, core) {

        core.initPermissions = function () {
            location.reload(); // FIXME: to refactor
        };

        console.getProfile = function () {
            return console.profile;
        };

        console.openUserLoginDialog = function (action) {
            var loginDialog = core.getView('#user-status-dialog', console.UserLoginDialog);
            if (!loginDialog) {
                core.getHtml(core.getComposumPath('composum/nodes/console/dialogs.user-status.html'),
                    _.bind(function (content) {
                        loginDialog = core.addLoadedDialog(console.UserLoginDialog, content);
                        if (loginDialog) {
                            if (_.isFunction(action)) {
                                action(loginDialog);
                            } else {
                                loginDialog.show();
                            }
                        }
                    }, this));
            } else {
                if (_.isFunction(action)) {
                    action(loginDialog);
                } else {
                    loginDialog.show();
                }
            }
        };

        console.authorize = function (retryThisFailedCall) {
            console.openUserLoginDialog(_.bind(function (loginDialog) {
                loginDialog.handleUnauthorized(retryThisFailedCall);
            }, this));
        };

        //
        // login and profile
        //

        console.profile = new core.LocalProfile('composum.core');

        console.UserLoginDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$content = this.$('.modal-content');
                this.$form = this.$('form');
                this.$login = this.$('button.login');
                this.$form.on('submit', _.bind(this.login, this));
                this.$login.click(_.bind(this.login, this));
                this.$el.on('shown.bs.modal', _.bind(this.onShown, this));
                this.callsToRetry = [];
                this.showing = false;
            },

            /**
             * collect all failed request calls to retry after successful login
             * @param retryThisFailedCall the call to retry after login
             */
            handleUnauthorized: function (retryThisFailedCall) {
                if (this.showing) {
                    // collect all failed calls during login
                    this.callsToRetry.push(retryThisFailedCall);
                } else {
                    // show login dialog and collect failed calls...
                    this.showing = true;
                    this.callsToRetry = [retryThisFailedCall];
                    this.show(undefined, _.bind(function () {
                        // retry after login all collected calls
                        this.callsToRetry.forEach(function (retryThisFailedCall) {
                            retryThisFailedCall();
                        });
                        this.showing = false;
                        this.callsToRetry = [];
                    }, this), 'authorization');
                }
            },

            show: function (initView, callback, type) {
                this.$content.removeClass();
                this.$content.addClass('modal-content');
                if (type) {
                    this.$content.addClass(type);
                }
                core.components.Dialog.prototype.show.apply(this, [initView, callback]);
            },

            /**
             * initialization after shown...
             */
            onShown: function () {
                this.$('input[name="j_username"]').focus();
            },

            login: function (event) {
                event.preventDefault();
                this.submitForm(_.bind(function () {
                        if (_.isFunction(this.callback)) {
                            this.hide();
                        } else {
                            core.initPermissions();
                        }
                    }, this),
                    _.bind(function (result) {
                        this.alert('danger', core.resultMessage(result));
                    }, this));
                return false;
            }
        });

        //
        // navbar
        //

        console.NavBar = Backbone.View.extend({

            el: $('header.navbar')[0],

            initialize: function () {
                var consoleId = $('body').attr('id');
                this.$('.nav-item a[data-redirect]').click(_.bind(this.dynamicRedirect, this));
                this.$permissionStatus = this.$('.nav-permissions-status');
                this.$permissionStatus.click(_.bind(this.togglePermission, this));
                this.showPermission();
                this.$('.nav-item.' + consoleId).addClass('active');
                this.$('.nav-user-status').on('click', _.bind(function () {
                    console.openUserLoginDialog();
                }, this));
                if (CPM.nodes && CPM.nodes.system) {
                    this.$status = this.$('.system-health-monitor');
                    this.$healthState = this.$status.find('span');
                    this.$status.click(_.bind(this.showStatus, this));
                    this.system = core.getWidget(this.$status, '.composum-nodes-system', CPM.nodes.system.Status);
                    $(document).on('system:health', _.bind(this.onSystemHealth, this));
                }
            },

            dynamicRedirect: function (event) {
                var $link = $(event.currentTarget);
                var redirectUrl = $link.data('redirect');
                if (redirectUrl) {
                    var currentPath = CPM.nodes.browser && _.isFunction(CPM.nodes.browser.getCurrentPath)
                        ? CPM.nodes.browser.getCurrentPath() : '';
                    var pathCondition = $link.data('path-condition');
                    var linkTarget = $link.attr('target');
                    redirectUrl = this.applyPlaceholder(redirectUrl, 'path', currentPath, pathCondition);
                    var contentPos = currentPath.indexOf('/jcr:content');
                    redirectUrl = this.applyPlaceholder(redirectUrl, 'editable',
                        contentPos > 0 ? currentPath.substring(0, contentPos) : currentPath, pathCondition);
                    window.open(redirectUrl, linkTarget ? linkTarget : '_self');
                    event.preventDefault();
                    return false;
                }
                return true;
            },

            applyPlaceholder: function (redirectUrl, key, value, condition) {
                return redirectUrl.replaceAll(new RegExp('\\${' + key + '(\\..+)?}', 'g'),
                    (value && (!condition || new RegExp(condition).exec(value))) ? (value + '$1') : '');
            },

            showPermission: function () {
                var user = this.$permissionStatus.data('user');
                var system = this.$permissionStatus.data('system');
                var status = (user && user !== 'none' ? 'user-' + user : 'default-' + system);
                core.removeClasses(this.$permissionStatus, /(user|default)-.+/).addClass(status);
                this.$permissionStatus.find('i').removeClass().addClass('fa fa-' + {
                    'default-read': 'shield',
                    'user-read': 'shield',
                    'default-write': 'wrench',
                    'user-write': 'wrench',
                }[status]);
                this.$permissionStatus.attr('title', status.replace(/-/, ': '));
            },

            togglePermission: function () {
                core.ajaxPost('/bin/cpm/core/restrictions.json', {}, {}, _.bind(function (result) {
                    window.location.reload();
                }, this));
            },

            onSystemHealth: function (event, status, data) {
                this.$healthState.removeClass().addClass(
                    'system-health-state system-health-' + (status ? status : 'unknown'));
            },

            showStatus: function () {
                if (this.$healthState.is('.system-health-state')) { // if status visible (accessible and loaded)...
                    core.openFormDialog(core.getComposumPath('composum/nodes/commons/components/system/dialog.html'),
                        CPM.nodes.system.StatusDialog);
                }
            }
        });

        console.navbar = new core.console.NavBar();

        //
        // detail view (generalized base class)
        //

        /**
         * the base 'class' for all detail tabs
         */
        console.DetailTab = Backbone.View.extend({

            /**
             * @abstract
             */
            reload: function () {
            }
        });

        /**
         * necessary initialization:
         * this.jobTopic
         * this.getCurrentPath()
         * this.$logOutput
         * this.$auditList
         * this.purgeAuditKeep
         */
        console.JobControlTab = console.DetailTab.extend({

            initialize: function (options) {
                console.DetailTab.prototype.initialize.apply(this, [options]);
            },

            reload: function () {
                this.setupStatus();
                this.resetAuditLog();
            },

            startJob: function (properties) {
                if (!this.currentJob) {
                    var path = this.getCurrentPath();
                    this.delay(); // cancel all open timeouts
                    this.logOffset = 0;
                    core.ajaxPost('/bin/cpm/core/jobcontrol.job.json', _.extend({
                            'event.job.topic': this.jobTopic,
                            'reference': path,
                            '_charset_': 'UTF-8'
                        }, properties), {},
                        _.bind(function (data, msg, xhr) {
                            this.jobStarted(data);
                            this.delay(true);
                        }, this),
                        _.bind(function (xhr) {
                            this.jobError('Job start failed: ', xhr);
                        }, this));
                }
            },

            cancelJob: function () {
                if (this.currentJob) {
                    core.ajaxDelete('/bin/cpm/core/jobcontrol.job.json/' + this.currentJob['slingevent:eventId'], {},
                        _.bind(function (data, msg, xhr) {
                            this.logAppend('Job cancellation requested...\n');
                            this.delay(true);
                        }, this),
                        _.bind(function (xhr) {
                            this.jobError('Job cancellation failed: ', xhr);
                        }, this));
                }
            },

            setupStatus: function () {
                var path = this.getCurrentPath();
                core.ajaxGet('/bin/cpm/core/jobcontrol.jobs.ACTIVE.json' + core.encodePath(path)
                    + '?topic=' + this.jobTopic, {},
                    _.bind(function (data, msg, xhr) {
                        if (data && data.length > 0) {
                            this.jobStarted(data[0]);
                            this.delay(true);
                        } else {
                            this.currentJob = undefined;
                        }
                    }, this),
                    _.bind(function (xhr) {
                        this.jobError('Job status check failed: ', xhr);
                    }, this));
            },

            jobStarted: function (job) {
                if (job) {
                    this.currentJob = job;
                    var operation = this.currentJob.operation;
                    this.logOffset = 0;
                    this.$logOutput.text('');
                    this.resetAuditLog();
                    core.removeClasses(this.$el, /.+-error/);
                    this.$el.addClass((operation ? operation : 'job') + '-running');
                }
            },

            jobStopped: function (message) {
                if (message) {
                    this.logAppend(message);
                }
                if (this.currentJob) {
                    core.removeClasses(this.$el, /.+-running/);
                    this.currentJob = undefined;
                }
            },

            jobError: function (text, xhr) {
                var operation = this.currentJob.operation;
                this.$el.addClass((operation ? operation : 'job') + '-error');
                this.jobStopped(text + xhr.statusText + (xhr.status ? (' (' + xhr.status + ')') : '') + '\n');
                this.delay(true); // probably one last 'pollOutput' after final status reached
            },

            jobSucceeded: function () {
                this.jobStopped('Job finished successfully.\n');
            },

            checkJob: function () {
                // pollOutput on each check even if 'currentJob' is undefined
                this.pollOutput(_.bind(function () {
                    if (this.currentJob) {
                        core.ajaxGet('/bin/cpm/core/jobcontrol.job.json/' + this.currentJob['slingevent:eventId'], {},
                            _.bind(function (data, msg, xhr) {
                                this.currentJob = data;
                                if (this.currentJob['slingevent:finishedState']) {
                                    switch (this.currentJob['slingevent:finishedState']) {
                                        case 'SUCCEEDED':
                                            this.jobSucceeded(); // extension hook
                                            break;
                                        case 'STOPPED':
                                            this.jobStopped('Job execution stopped.\n');
                                            break;
                                        case 'ERROR':
                                            this.jobError("Job finished with 'Error': ", {
                                                statusText: this.currentJob['slingevent:resultMessage']
                                            });
                                            break;
                                        default:
                                            this.jobStopped("Job execution finished with '" +
                                                this.currentJob['slingevent:finishedState'] + "'.\n");
                                            break;
                                    }
                                }
                            }, this),
                            _.bind(function (xhr) {
                                this.jobError('Job status check failed: ', xhr);
                            }, this));
                        this.delay(true);
                    }
                }, this));
            },

            pollOutput: function (callback, jobId) {
                if (!jobId) {
                    if (this.currentJob) {
                        jobId = this.currentJob['slingevent:eventId'];
                    }
                }
                if (jobId) {
                    core.ajaxGet('/bin/cpm/core/jobcontrol.outfile.txt/' + jobId, {
                            headers: {
                                Range: 'bytes=' + this.logOffset + '-' // get all output from last offset
                            }
                        },
                        _.bind(function (data, msg, xhr) {
                            var contentLength = parseInt(xhr.getResponseHeader('Content-Length'));
                            this.logAppend(data);
                            this.logOffset += contentLength;
                            if (_.isFunction(callback)) {
                                callback.call(this);
                            }
                        }, this),
                        _.bind(function (xhr) {
                            this.logAppend('Job output retrieval failed: '
                                + xhr.statusText + ' (' + xhr.status + ')\n');
                            if (_.isFunction(callback)) {
                                callback.call(this);
                            }
                        }, this));
                }
            },

            logAppend: function (data) {
                if (data) {
                    var $scrollPane = this.$logOutput.parent();
                    var vheight = $scrollPane.height();
                    var height = this.$logOutput[0].scrollHeight;
                    var autoscroll = ($scrollPane.scrollTop() > height - vheight - 30);
                    this.$logOutput.text(this.$logOutput.text() + data);
                    if (autoscroll) {
                        height = this.$logOutput[0].scrollHeight;
                        $scrollPane.scrollTop(height - vheight);
                    }
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

            resetAuditLog: function () {
                this.$auditList.find('li').removeClass('current');
            },

            loadAuditLog: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.$auditList.html('');
                var path = this.getCurrentPath();
                core.ajaxGet('/bin/cpm/core/jobcontrol.jobs.ALL.json' + core.encodePath(path)
                    + '?topic=' + this.jobTopic, {},
                    _.bind(function (data, msg, xhr) {
                        for (var i = 0; i < data.length; i++) {
                            var state = data[i].jobState;
                            this.$auditList.append('<li class="'
                                + (state ? state.toLowerCase() : 'unknown')
                                + (data[i]['operation'] ? (' ' + data[i]['operation']) : '') + '">'
                                + '<a href="#" data-id="' + data[i]['slingevent:eventId'] + '">'
                                + '<span class="time created">' + data[i]['slingevent:created'] + '</span>'
                                + '<span class="state">' + state + '</span>'
                                + '<span class="time finished">' + data[i]['slingevent:finishedDate'] + '</span>'
                                + (data[i]['operation'] ? ('<span class="operation">' + data[i]['operation'] + '</span>') : '')
                                + '<span class="result">' + data[i]['slingevent:resultMessage'] + '</span>'
                                + '</a></li>');
                        }
                        this.$auditList.find('a').click(_.bind(this.loadAuditLogfile, this));
                    }, this),
                    _.bind(function (xhr) {
                        this.logAppend('Audit Log load failed: ' + xhr.statusText);
                    }, this));
            },

            loadAuditLogfile: function (event) {
                event.preventDefault();
                this.resetAuditLog();
                var $link = $(event.currentTarget);
                $link.closest('li').addClass('current');
                this.$logOutput.text('');
                this.logOffset = 0;
                this.pollOutput(_.bind(function () {
                    this.logAppend($link.find('.result').text());
                }, this), $link.data('id'));
            },

            purgeAuditLog: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var dialog = console.getPurgeAuditDialog();
                dialog.show(_.bind(function () {
                    dialog.initDialog(this.jobTopic,
                        this.getCurrentPath(),
                        this.purgeAuditKeep ? this.purgeAuditKeep : 3);
                }, this), _.bind(function () {
                    this.loadAuditLog();
                }, this));
            }
        });

        /**
         * the node view (node detail) which controls the node view tabs
         */
        console.DetailView = Backbone.View.extend({

            // 'abstract' members...

            getProfileId: function () {
                return undefined;
            },

            getCurrentPath: function () {
                return undefined;
            },

            getViewUri: function () {
                return undefined;
            },

            getTabUri: function (name) {
                return undefined;
            },

            getTabTypes: function () {
                return [{
                    // the fallback to the basic implementation as a default rule
                    selector: '> div',
                    tabType: console.DetailTab
                }];
            },

            // detail view implementation

            initialize: function (options) {
                $(document).on('path:selected.DetailView', _.bind(this.reload, this));
            },

            /**
             * (re)load the content with the view for the current node ('console.getCurrentPath()')
             */
            reload: function () {
                this.path = this.getCurrentPath();
                if (this.path) {
                    if (!this.busy) {
                        this.busy = true;
                        try {
                            // AJAX load for the current path with the 'viewUrl' from 'console.current'
                            this.$el.load(this.getViewUri(), _.bind(function () {
                                // initialize detail view with the loaded content
                                this.$detailView = this.$('.detail-view');
                                this.$detailTabs = this.$detailView.find('.detail-tabs');
                                this.$detailContent = this.$detailView.find('.detail-content');
                                // add the click handler to the tab toolbar links
                                this.$detailTabs.find('a').click(_.bind(this.tabSelected, this));
                                // get the group key of the last view from profile and restore this tab state if possible
                                var group = core.console.getProfile().get(this.getProfileId(), 'detailTab');
                                var $tab;
                                if (group) {
                                    // determinte the last view by the group id if such a view is available
                                    $tab = this.$detailView.find('a[data-group="' + group + '"]');
                                }
                                if (!$tab || $tab.length < 1) {
                                    if (group === 'edit') { // special fallback from 'edit' to 'view
                                        $tab = this.$detailView.find('a[data-group="view"]');
                                    }
                                    if (!$tab || $tab.length < 1) {
                                        // if the group of the last view is not available use the view of the first tab
                                        $tab = this.$detailTabs.find('a');
                                    }
                                }
                                // get the tab key from the links anchor and select the tab
                                var tab = $tab.attr('href').substring(1);
                                this.selectTab(tab, group); // remember the group key(!)
                                if (_.isFunction(this.onReload)) {
                                    this.onReload();
                                }
                            }, this));
                        } finally {
                            this.busy = false;
                        }
                    }
                } else {
                    this.$el.html(''); // clear the view if nothing selected
                }
            },

            /**
             * select a tab by the tabs anchor name; remember the group of the tab in the users profile,
             * remember the group given here as parameter if this parameter is not undefined
             */
            selectTab: function (name, group) {
                if (!name) {
                    name = this.currentTab || 'properties';
                }
                this.refreshContent(name, _.bind(function () {
                    this.$detailTabs.find('a.active').removeClass('active');
                    var $item = this.$detailTabs.find('a[href="#' + name + '"]');
                    $item.addClass('active');
                    if (!group) {
                        group = $item.attr('data-group');
                    }
                    core.console.getProfile().set(this.getProfileId(), 'detailTab', group);
                    this.currentTab = name;
                }, this));
            },

            /**
             * the event handler for the tab actions (button links) calls 'selectTab' with the links anchor
             */
            tabSelected: function (event) {
                event.preventDefault();
                var $action = $(event.currentTarget).closest('a');
                var tab = $action.attr('href').substring(1);
                this.selectTab(tab);
                return false;
            },

            refreshContent: function (name, refreshTabState, parameters) {
                if (!name) {
                    name = this.currentTab || 'properties';
                }
                var path = this.getCurrentPath();
                if (name && path) {
                    var uri = new core.SlingUrl(this.getTabUri(name) + core.encodePath(path), parameters);
                    this.$detailContent.load(core.getContextUrl(uri.build()), _.bind(function () {
                        if (_.isFunction(refreshTabState)) {
                            refreshTabState();
                        }
                        // initialize the new view
                        this.viewWidget = undefined;
                        var tabTypes = this.getTabTypes();
                        for (var i = 0; !this.viewWidget && i < tabTypes.length; i++) {
                            var type = tabTypes[i];
                            if (_.isFunction(type)) {
                                type = type(this.$detailContent);
                            }
                            this.viewWidget = core.getWidget(this.$detailContent, type.selector, type.tabType);
                        }
                        if (this.viewWidget) {
                            if (_.isFunction(this.viewWidget.reload)) {
                                this.viewWidget.reload();
                            }
                        }
                    }, this));
                }
            }
        });

    })(CPM.console, CPM.core);

})();
