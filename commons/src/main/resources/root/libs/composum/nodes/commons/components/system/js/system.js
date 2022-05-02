/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.system');

    (function (system, core) {

        system.const = _.extend(system.const || {}, {
            css: {
                base: 'composum-nodes-system',
                _users: '_users',
                _health: '_health'
            },
            felix: {
                css: {
                    base: 'composum-nodes-system-felix',
                    _tags: '_tags',
                    _refresh: '_refresh',
                    _content: '_content'
                },
                url: {
                    status: '/bin/cpm/proxy.fwd/system/health.json?httpStatus=CRITICAL:200,TEMPORARILY_UNAVAILABLE:200',
                    content: '/bin/cpm/proxy.fwd/system/health.html?httpStatus=CRITICAL:200,TEMPORARILY_UNAVAILABLE:200&tags=',
                    polling: 300000 // every 5 minutes
                }
            }
        });

        system.FelixHealthView = Backbone.View.extend({

            initialize: function (options) {
                var c = system.const.felix.css;
                this.tags = core.getWidget(this.$el, '.' + c.base + c._tags, core.components.TextFieldWidget);
                this.tags.setValue(CPM.nodes.profile.get('system', 'felixTags'));
                this.tags.changed('View', _.bind(function (event, value) {
                    CPM.nodes.profile.set('system', 'felixTags', value);
                }, this));
                this.$('.' + c.base + c._refresh).click(_.bind(this.refresh, this));
                this.$content = this.$('.' + c.base + c._content);
                this.refresh();
            },

            refresh: function () {
                core.getHtml(system.const.felix.url.content + encodeURIComponent(this.tags.getValue()),
                    _.bind(function (content) {
                        this.$content.html(content);
                        this.$content.find('table').addClass('table table-condensed');
                    }, this));
            }
        });

        system.StatusForm = core.components.FormWidget.extend({

            initialize: function (options) {
                core.components.FormWidget.prototype.initialize.call(this, options);
                this.felixHealth = core.getWidget(this.$el, '.' + system.const.felix.css.base, system.FelixHealthView);
                this.tabbed.$nav.find('a[data-key="' + CPM.nodes.profile.get('system', 'dialogTab', 'felix') + '"]').tab('show');
                this.tabbed.$nav.find('a').on('shown.bs.tab.FormTabs', _.bind(function (event) {
                    var $tab = $(event.target);
                    CPM.nodes.profile.set('system', 'dialogTab', $tab.data('key'));
                }, this));
            },

            refresh: function () {
                this.felixHealth.refresh();
            }
        });

        system.StatusDialog = core.components.FormDialog.extend({

            initialize: function (options) {
                core.components.FormDialog.prototype.initialize.call(this, _.extend({
                    formType: system.StatusForm
                }, options));
            },

            /**
             * suppress each submit
             */
            onSubmit: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.form.refresh();
                return false;
            }
        });

        system.Status = Backbone.View.extend({

            initialize: function (options) {
                var c = system.const.css;
                this.data = {};
                this.$users = this.$('.' + c.base + c._users);
                this.$usersCount = this.$users.find('span');
                this.$health = this.$('.' + c.base + c._health);
                this.$healthState = this.$health.find('span');
                this.refresh();
            },

            refresh: function () {
                this.healthStatus(_.bind(function () {
                    this.$healthState.removeClass().addClass('system-health-' + (this.data.health
                        ? this.data.health.overallResult : 'unknown'));
                    core.i18n.get(this.data.healthState, _.bind(function (text) {
                        this.$healthState.text(text);
                    }, this));
                    window.setTimeout(_.bind(this.refresh, this), system.const.felix.url.polling);
                }, this));
            },

            healthStatus: function (callback) {
                core.getJson(system.const.felix.url.status, _.bind(function (data) {
                    this.data.health = data;
                    if (this.data.healthState !== data.overallResult) {
                        this.data.healthState = data.overallResult;
                        $(document).trigger('system:health', [data.overallResult, data]);
                    }
                    if (_.isFunction(callback)) {
                        callback(data);
                    }
                }, this));
            }
        });

    })(CPM.nodes.system, CPM.core);

})();
