/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.Query = Backbone.View.extend({

            queryParamsPattern: new RegExp('^[^$]*(\\${[^}]+})([^$]*(\\${[^}]+}))?([^$]*(\\${[^}]+}))?([^$]*(\\${[^}]+}))?.*$'),
            paramNamePattern: new RegExp('^\\${([^}]+)}$'),

            initialize: function (options) {
                this.$povHook = this.$('.popover-hook');
                this.$form = this.$('.query-actions .query-input-form');
                this.$templates = this.$('.query-actions .templates');
                this.$history = this.$('.query-actions .history');
                this.$queryInput = this.$('.query-actions .query-input-form input');
                this.$execButton = this.$('.query-actions .exec');
                this.$filterButton = this.$('.query-actions .filter');
                this.$exportMenu = this.$('.query-actions .query-export');
                this.$resultTable = this.$('.query-result table');
                this.$form.on('submit', _.bind(this.executeQuery, this));
                this.$templates.on('click.query', _.bind(this.showTemplates, this));
                this.$history.on('click.query', _.bind(this.showHistory, this));
                this.$execButton.on('click.query', _.bind(this.executeQuery, this));
                this.$filterButton.on('click.query', _.bind(this.toggleFilter, this));
                this.$filterButton.addClass(core.console.getProfile().get('query', 'filtered', true) ? 'on' : 'off');
                this.$exportMenu.on('show.bs.dropdown', _.bind(this.showExportMenu, this));
                this.$queryInput.on('focus.reset', _.bind(this.hidePopover, this));
                this.$queryInput.on('keypress.exec', _.bind(function (event) {
                    if (event.which === 13) {
                        this.executeQuery(event);
                    }
                }, this));
                this.$queryInput.on('keyup.validate', _.bind(this.queryUpdated, this));
                this.$queryInput.on('change.validate', _.bind(this.queryUpdated, this));
                this.$queryInput.val(core.console.getProfile().get('query', 'current'));
                this.queryUpdated();
            },

            executeQuery: function (event) {
                event.preventDefault();
                this.hidePopover();
                this.$resultTable.html('<tbody><tr><td class="pulse"><i class="fa fa-spinner fa-pulse"></i></td></tr></tbody>');
                var query = this.$queryInput.val();
                core.ajaxGet('/bin/cpm/nodes/node.query.html', {
                    data: {
                        query: this.prepareQuery(query),
                        filter: this.isFiltered() ? browser.tree.filter : ''
                    }
                }, _.bind(function (content) {
                    this.memorizeQuery(query);
                    this.$resultTable.html(content);
                    var queryWidget = this;
                    this.$resultTable.find('td.icon').each(function () {
                        var $td = $(this);
                        var typeHint = $td.attr('data-type');
                        var rule = core.components.treeTypes[typeHint];
                        if (!rule) {
                            rule = core.components.treeTypes['default'];
                        }
                        $td.find('span').addClass(rule.icon)
                    });
                    var empty = true;
                    this.$resultTable.find('td.path').each(function () {
                        empty = false;
                        var $td = $(this);
                        $td.find('a').on('click', _.bind(queryWidget.pathSelected, queryWidget));
                    });
                }, this), _.bind(function (result) {
                    var message = core.resultMessage(result, 'error on execute query');
                    this.$resultTable.html('<tbody><tr><td class="error danger">' + message + '</td></tr></tbody>');
                }, this));
                return false;
            },

            prepareQuery: function (query) {
                if (this.parameters && this.parameters.length > 0) {
                    for (var i = 0; i < this.parameters.length; i++) {
                        query = query.replace('${' + this.parameters[i] + '}',
                            this.$('.param-input-line .form-inline [data-key="' + this.parameters[i] + '"] input').val());
                    }
                }
                return query;
            },

            showExportMenu: function () {
                core.ajaxGet('/libs/composum/nodes/browser/query/export/set.html', {
                        data: {
                            query: this.prepareQuery(this.$queryInput.val()),
                            filter: this.isFiltered() ? browser.tree.filter : ''
                        }
                    },
                    _.bind(function (content) {
                        var $menu = this.$exportMenu.find('ul');
                        $menu.html(content);
                        $menu.find('input[name="query"]').each(function () {
                            var $this = $(this);
                            var value = atob($this.attr('value'));
                            $this.attr('value', value);
                        });
                        $menu.find('.query-export-link').click(_.bind(function (event) {
                            var $link = $(event.currentTarget);
                            var $form = $link.closest('form');
                            $form.submit();
                        }, this));
                    }, this)
                );
            },

            queryUpdated: function (event) {
                var query = this.$queryInput.val();
                core.console.getProfile().set('query', 'current', query);
                var parameters = this.queryParamsPattern.exec(query);
                var newParams = undefined;
                if (parameters) {
                    newParams = [];
                    newParams[0] = this.paramNamePattern.exec(parameters[1])[1];
                    if (parameters.length > 3 && parameters[3]) {
                        newParams[1] = this.paramNamePattern.exec(parameters[3])[1];
                    }
                    if (parameters.length > 5 && parameters[5]) {
                        newParams[2] = this.paramNamePattern.exec(parameters[5])[1];
                    }
                    if (parameters.length > 7 && parameters[7]) {
                        newParams[3] = this.paramNamePattern.exec(parameters[7])[1];
                    }
                }
                if (!_.isEqual(this.parameters, newParams)) {
                    this.parameters = newParams;
                    if (newParams && newParams.length > 0) {
                        this.$el.addClass('parameters');
                    } else {
                        this.$el.removeClass('parameters');
                    }
                    var $line = this.$('.param-input-line');
                    var $form = $line.find('.form-inline');
                    $form.html('');
                    if (newParams) {
                        for (var i = 0; i < newParams.length; i++) {
                            var label = /^([^.]+)(\..+)?$/.exec(newParams[i]);
                            if (label && $form.find('.input-group[data-key="' + newParams[i] + '"]').length === 0) {
                                label = label[1].replace(/_/g, ' ');
                                var type = /\.(path)(\.[\d]+)?$/.exec(newParams[i]);
                                var grow = /\.([\d]+)$/.exec(newParams[i]);
                                var templateClass = 'template' + (type ? ('-' + type[1]) : '');
                                var $template = $line.find('.' + templateClass);
                                var $input = $template.clone();
                                var $inputField = $input.find('input');
                                $input.attr('data-key', newParams[i]);
                                $input.find('.key').text(label);
                                if (grow) {
                                    $input.css({'flex-grow': grow[1]});
                                }
                                var $pathSelect = $input.find('.path-select');
                                if ($pathSelect.length > 0) {
                                    $pathSelect.on('click', function (event) {
                                        var $group = $(event.currentTarget).closest('.input-group');
                                        var label = $group.find('.key').text();
                                        var $field = $group.find('input');
                                        var selectDialog = core.getView('#path-select-dialog', core.components.SelectPathDialog);
                                        selectDialog.setTitle(label);
                                        selectDialog.show(function () {
                                            selectDialog.setValue($field.val());
                                        }, function () {
                                            $field.val(selectDialog.getValue());
                                        });
                                    });
                                }
                                $inputField.focus(_.bind(this.hidePopover, this));
                                $inputField.keypress(_.bind(function (event) {
                                    if (event.which === 13) {
                                        this.executeQuery(event);
                                    }
                                }, this));
                                $input.removeClass(templateClass);
                                $form.append($input);
                            }
                        }
                    }
                }
            },

            memorizeQuery: function (query) {
                var queries = core.console.getProfile().get('query', 'history', []);
                queries = _.without(queries, query); // remove existing entry
                queries = _.union([query], queries); // insert query at first pos
                queries = _.first(queries, 30); // restrict history to 30 entries
                core.console.getProfile().set('query', 'history', queries);
            },

            querySelected: function (event) {
                event.preventDefault();
                var $target = $(event.currentTarget);
                this.$queryInput.val($target.text());
                this.queryUpdated();
            },

            pathSelected: function (event) {
                event.preventDefault();
                var $target = $(event.currentTarget);
                var $row = $target.closest('tr');
                var path = $row.attr('data-path');
                $(document).trigger("path:select", [path]);
                return false;
            },

            isFiltered: function () {
                return this.$filterButton.hasClass('on');
            },

            toggleFilter: function (event) {
                event.preventDefault();
                if (this.isFiltered()) {
                    this.$filterButton.removeClass('on');
                    this.$filterButton.addClass('off');
                } else {
                    this.$filterButton.removeClass('off');
                    this.$filterButton.addClass('on');
                }
                core.console.getProfile().set('query', 'filtered', this.isFiltered());
                return false;
            },

            showHistory: function (event) {
                event.preventDefault();
                if (this.popover === 'history') {
                    this.$povHook.popover('destroy');
                    this.popover = undefined;
                } else {
                    var content = '';
                    var queries = core.console.getProfile().get('query', 'history', []);
                    if (queries) {
                        content += '<ul class="template-links">';
                        for (var i = 0; i < queries.length; i++) {
                            content += ('<li><a href="#" class="query-template">' + queries[i] + '</a></li>');
                        }
                        content += '</ul>';
                    }
                    this.showPopover('history', this.$history.attr('title'), content);
                }
                return false;
            },

            showTemplates: function (event) {
                event.preventDefault();
                if (this.popover === 'templates') {
                    this.$povHook.popover('toggle');
                } else {
                    core.getHtml('/libs/composum/nodes/browser/query/templates.html', _.bind(function (data) {
                        this.showPopover('templates', this.$templates.attr('title'), data);
                    }, this));
                }
                return false;
            },

            showPopover: function (key, title, content) {
                this.$povHook.popover('destroy');
                this.$povHook.off()
                    .on('inserted.bs.popover', _.bind(this.initPopover, this))
                    .on('shown.bs.popover', _.bind(this.onPopoverShown, this))
                    .on('hidden.bs.popover', _.bind(this.onPopoverHidden, this));
                this.$povHook.popover({
                    title: title,
                    placement: 'bottom',
                    animation: false,
                    html: true,
                    sanitize: false,
                    content: content
                });
                this.popover = key;
                this.$povHook.popover('show');
            },

            initPopover: function (event) {
                var id = this.$povHook.attr('aria-describedby');
                var $popover = $('#' + id);
                $popover.find('.template-links a').click(_.bind(function (event) {
                    event.preventDefault();
                    var $link = $(event.currentTarget);
                    var $template = $link.closest('.query-template');
                    var string = $template.data('encoded');
                    var query;
                    if (string) {
                        var data = JSON.parse(atob(string));
                        query = data[$link.data('type')];
                    } else {
                        query = $link.text();
                    }
                    this.$queryInput.val(query);
                    this.queryUpdated();
                    this.hidePopover();
                    return false;
                }, this));
            },

            hidePopover: function () {
                if (this.popover === 'templates') {
                    this.$povHook.popover('hide');
                } else {
                    this.$povHook.popover('destroy');
                    this.popover = undefined;
                }
            },

            onPopoverShown: function () {
                if (this.popover === 'templates') {
                    this.$templates.addClass('active')
                } else {
                    this.$history.addClass('active')
                }
            },

            onPopoverHidden: function () {
                this.$templates.removeClass('active');
                this.$history.removeClass('active');
            }
        });

        browser.query = core.getView('#browser-query .query-panel', browser.Query);

    })(core.browser);

})(window.core);
