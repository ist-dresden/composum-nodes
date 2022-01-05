/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.browser.references');

    (function (references, browser, core) {

        references.getTab = function () {
            return core.getView('.node-view-panel .references', references.Tab);
        };

        references.Tab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$toolbar = this.$('.references-toolbar');
                this.$content = this.$('.detail-content');
                core.getWidget(this.$toolbar, '.widget.path-widget', core.components.PathWidget, {
                    selector: {
                        button: '.input-group-addon.select'
                    }
                });
                var profile = this.getOptions();
                this.setOptions(profile);
                this.$toolbar.find('input[type="text"]').change(_.bind(this.applyTextOption, this));
                this.$toolbar.find('input[type="checkbox"]').change(_.bind(this.applyOptionFlag, this));
                this.$toolbar.find('.options').click(_.bind(this.openOptions, this));
                this.$toolbar.find('.refresh').click(_.bind(this.reload, this));
            },

            initContent: function () {
                this.$content.find('.references-list_hit_toggle').click(this.toggleHit);
                this.$content.find('.references-list_hit_link').click(this.jumpToReferer);
            },

            toggleHit: function (event) {
                event.preventDefault();
                $(event.currentTarget).closest('.references-list_hit').toggleClass('open');
                return false;
            },

            jumpToReferer: function (event) {
                event.preventDefault();
                browser.setCurrentPath($(event.currentTarget).data('path'));
                return false;
            },

            applyTextOption: function (event) {
                var $control = $(event.currentTarget);
                var value = $control.val();
                var options = this.getOptions();
                options[$control.attr('name')] = value ? value : '';
                this.setOptions(options);
            },

            applyOptionFlag: function (event) {
                var $checkbox = $(event.currentTarget);
                var name = $checkbox.attr('name');
                var options = this.getOptions(true);
                options[name] = !!$checkbox.prop('checked');
                this.setOptions(options);
            },

            getOptions: function (adjustPaths) {
                var options = core.console.getProfile().get('references', 'options', {
                    root: '/content',
                    abs: true,
                    text: true,
                    rich: true
                });
                if (adjustPaths) {
                    this.$toolbar.find('input[type="text"]').each(function () {
                        var $control = $(this);
                        var value = $control.val();
                        if (options[$control.attr('name')] !== value) {
                            options[$control.attr('name')] = value;
                            core.console.getProfile().set('references', 'options', options);
                        }
                    });
                }
                return options;
            },

            setOptions: function (options) {
                core.console.getProfile().set('references', 'options', options);
                this.$toolbar.find('input[type="text"]').each(function () {
                    var $control = $(this);
                    $control.val(options[$control.attr('name')]);
                });
                this.$toolbar.find('input[type="checkbox"]').each(function () {
                    var $control = $(this);
                    $control.prop('checked', !!options[$control.attr('name')]);
                });
                this.reload();
            },

            openOptions: function () {
                core.openLoadedDialog('/libs/composum/nodes/browser/components/references/options.dialog.html',
                    references.Options, undefined, _.bind(function (dialog) {
                        var options = this.getOptions(true);
                        dialog.$('input[type="text"]').each(function () {
                            var $control = $(this);
                            $control.val(options[$control.attr('name')]);
                        });
                        dialog.$('input[type="checkbox"]').each(function () {
                            var $control = $(this);
                            $control.prop('checked', !!options[$control.attr('name')]);
                        });
                    }, this), _.bind(function (dialog) {
                        var options = this.getOptions();
                        dialog.$('input[type="text"]').each(function () {
                            var $control = $(this);
                            options[$control.attr('name')] = $control.val();
                        });
                        dialog.$('input[type="checkbox"]').each(function () {
                            var $control = $(this);
                            options[$control.attr('name')] = !!$control.prop('checked');
                        });
                        this.setOptions(options);
                    }, this));
            },

            reload: function () {
                this.$el.addClass('loading');
                core.ajaxGet('/libs/composum/nodes/browser/components/references.content.html'
                    + browser.getCurrentPath(), {
                    data: _.extend({_charset_: 'UTF-8'}, this.getOptions(true))
                }, _.bind(function (content) {
                    this.$content.html(content);
                    this.initContent();
                }, this), undefined, _.bind(function () {
                    this.$el.removeClass('loading');
                }, this));
            }
        });

        references.Options = core.components.LoadedDialog.extend({

            initialize: function (options) {
                core.components.LoadedDialog.prototype.initialize.call(this, options);
                this.$('button.apply').click(_.bind(function () {
                    this.hide();
                }, this));
            }
        });

    })(CPM.nodes.browser.references, CPM.nodes.browser, CPM.core);

})();
