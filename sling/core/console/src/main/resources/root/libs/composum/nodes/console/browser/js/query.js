/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.Query = Backbone.View.extend({

            queryParamsPattern: new RegExp('^[^\\$]*(\\$\\{[^\\}]+\\})([^\\$]*(\\$\\{[^\\}]+\\}))?([^\\$]*(\\$\\{[^\\}]+\\}))?([^\\$]*(\\$\\{[^\\}]+\\}))?.*$'),
            paramNamePattern: new RegExp('^\\$\\{([^\\}]+)\\}$'),

            initialize: function (options) {
                this.loadTemplates();
                window.setTimeout(_.bind(this.setupQueriesMenu, this), 500);
                this.$form = this.$('.query-actions form');
                this.$selectMenu = this.$('.query-actions ul.select');
                this.$queryInput = this.$('.query-actions form input');
                this.$execButton = this.$('.query-actions .exec');
                this.$filterButton = this.$('.query-actions .filter');
                this.$resultTable = this.$('.query-result table');
                this.$form.on('submit', _.bind(this.executeQuery, this));
                this.$execButton.on('click.query', _.bind(this.executeQuery, this));
                this.$filterButton.on('click.query', _.bind(this.toggleFilter, this));
                this.$filterButton.addClass(core.console.getProfile().get('query', 'filtered', true) ? 'on' : 'off');
                this.$queryInput.on('keyup.validate', _.bind(this.queryUpdated, this));
                this.$queryInput.on('change.validate', _.bind(this.queryUpdated, this));
                this.$queryInput.val(core.console.getProfile().get('query', 'current'));
                this.queryUpdated();
            },

            executeQuery: function (event) {
                event.preventDefault();
                this.$resultTable.html('<tbody><tr><td class="pulse"><i class="fa fa-spinner fa-pulse"></i></td></tr></tbody>');
                var query = this.$queryInput.val();
                core.ajaxGet('/bin/cpm/nodes/node.query.html', {
                    data: {
                        query: this.prepareQuery(query),
                        filter: this.isFiltered() ? browser.tree.filter : ''
                    }
                }, _.bind(function (result) {
                    this.memorizeQuery(query);
                    this.setupQueriesMenu();
                    this.$resultTable.html(result);
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
                        this.$el.removeClass('params-1 params-2 params-3 params-4');
                        this.$el.addClass('parameters params-' + newParams.length);
                    } else {
                        this.$el.removeClass('parameters params-1 params-2 params-3 params-4');
                    }
                    var $line = this.$('.param-input-line');
                    var $form = $line.find('.form-inline');
                    $form.html('');
                    if (newParams) {
                        var $template = $line.find('.template');
                        for (var i = 0; i < newParams.length; i++) {
                            var $input = $template.clone();
                            $input.attr('data-key', newParams[i]);
                            $input.find('.key').text(newParams[i]);
                            $input.removeClass('template');
                            $form.append($input);
                        }
                    }
                }
            },

            memorizeQuery: function (query) {
                var queries = core.console.getProfile().get('query', 'history', this.queryTemplates);
                queries = _.without(queries, query); // remove existing entry
                queries = _.union([query], queries); // insert query at first pos
                queries = _.first(queries, 12); // restrict history to 12 entries
                core.console.getProfile().set('query', 'history', queries);
            },

            setupQueriesMenu: function () {
                this.$selectMenu.html('');
                var queries = core.console.getProfile().get('query', 'history', this.queryTemplates);
                if (queries) {
                    for (var i = 0; i < queries.length; i++) {
                        this.$selectMenu.append('<li><a href="#">' + queries[i] + '</a></li>');
                    }
                }
                this.$selectMenu.find('li>a').on('click', _.bind(this.querySelected, this));
                this.$selectMenu.append('<li role="separator" class="divider"></li>');
                this.$selectMenu.append('<li><a href="#" class="clear">clear history</a></li>');
                this.$selectMenu.find('li>a.clear').on('click', _.bind(function (event) {
                    event.preventDefault();
                    core.console.getProfile().set('query', 'history', undefined);
                    this.setupQueriesMenu();
                }, this));
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

            loadTemplates: function () {
                core.getJson("/bin/cpm/nodes/node.queryTemplates.json", _.bind(function (templates) {
                    this.queryTemplates = templates;
                }, this));
            }
        });

        browser.query = core.getView('#browser-query .query-panel', browser.Query);

    })(core.browser);

})(window.core);
