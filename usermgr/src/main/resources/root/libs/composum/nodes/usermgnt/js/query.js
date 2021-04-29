(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, graph, core) {

        usermanagement.const = _.extend(usermanagement.const || {}, {
            icon: {
                'group': 'fa-users',
                'service': 'fa-cog',
                'system': 'fa-user-o',
                'user': 'fa-user'
            }
        });

        usermanagement.Query = Backbone.View.extend({

            initialize: function (options) {
                this.$queryMode = this.$('.query-mode');
                this.$form = this.$('.query-actions form');
                this.$queryInput = this.$('.query-actions form input');
                this.$execButton = this.$('.query-actions .exec');
                this.$resultTable = this.$('.query-result table');
                this.$queryCanvas = this.$('.query-result .query-canvas');
                var formData = core.console.getProfile().get('usermgr', 'queryForm', {});
                this.$form.find('[name="type"]').val(formData.type);
                this.$form.find('[name="name"]').val(formData.name);
                this.$form.find('[name="path"]').val(formData.path);
                this.$form.find('[name="text"]').val(formData.text);
                this.$queryMode.find('button').click(_.bind(this.selectMode, this));
                this.$form.on('submit.query', _.bind(this.executeQuery, this));
                this.$form.find('[name="type"]').on('change.query', _.bind(this.executeQuery, this));
                this.$execButton.on('click.query', _.bind(this.executeQuery, this));
                this.selectMode(undefined, core.console.getProfile().get('usermgr', 'queryMode', 'users'));
            },

            selectMode: function (event, mode) {
                if (event) {
                    event.preventDefault();
                    if (!mode) {
                        mode = $(event.currentTarget).data('mode');
                    }
                    core.console.getProfile().set('usermgr', 'queryMode', mode);
                }
                if (!mode) {
                    mode = 'users'
                }
                this.currentMode = mode;
                this.$queryMode.find('button').removeClass('active');
                this.$queryMode.find('button.' + this.currentMode).addClass('active');
                if (this.currentMode === 'users') {
                    this.$queryCanvas.addClass('hidden');
                    this.$resultTable.removeClass('hidden');
                } else {
                    this.$resultTable.addClass('hidden');
                    this.$queryCanvas.removeClass('hidden');
                }
                if (this.currentMode === 'paths') {
                    this.$form.find('[name="text"]').removeClass('hidden');
                } else {
                    this.$form.find('[name="text"]').addClass('hidden');
                }
                this.executeQuery(event);
                return false;
            },

            executeQuery: function (event) {
                if (event) {
                    event.preventDefault();
                }
                var mode = this.currentMode;
                var query = '';
                var queryProfile = {};
                var formData = new FormData(this.$form[0]);
                formData.forEach(function (value, key) {
                    var pattern = '' + value;
                    if (pattern) {
                        if (key !== 'text' || mode === 'paths') {
                            query += (query.length < 1 ? '?' : '&') + key + '=' + encodeURIComponent(value);
                        }
                        queryProfile[key] = '' + pattern;
                    }
                });
                if (event || query) {
                    core.console.getProfile().set('usermgr', 'queryForm', queryProfile);
                    switch (this.currentMode) {
                        default:
                        case 'users':
                            this.executeUsersQuery(query);
                            break;
                        case 'paths':
                            this.executePathsQuery(query);
                            break;
                        case 'graph':
                            this.executeGraphQuery(query);
                            break;
                    }
                    return false;
                }
            },

            executeGraphQuery: function (query) {
                graph.execute(this.$queryCanvas, query, 'view.graphviz', _.bind(function () {
                    this.$queryCanvas.find('svg').find('a').click(_.bind(function (event) {
                        const path = $(event.currentTarget).attr('title');
                        if (path) {
                            event.preventDefault();
                            usermanagement.setCurrentPath(path);
                            return false;
                        }
                        return true;
                    }, this));
                }, this));
            },

            executePathsQuery: function (query) {
                graph.execute(this.$queryCanvas, query, 'view.paths', _.bind(function () {
                    this.$queryCanvas.find('td.authorizable-id').find('a').click(_.bind(function (event) {
                        const path = $(event.currentTarget).data('path');
                        if (path) {
                            event.preventDefault();
                            usermanagement.setCurrentPath(path);
                            return false;
                        }
                        return true;
                    }, this));
                }, this));
            },

            executeUsersQuery: function (query) {
                this.$resultTable.html('<tbody><tr><td class="pulse"><i class="fa fa-spinner fa-pulse"></i></td></tr></tbody>');
                core.ajaxGet('/bin/cpm/usermanagement.query.json' + query, {
                    dataType: 'json'
                }, _.bind(function (result) {
                    var queryWidget = this;
                    var row = _.template(
                        '<tr class="<%= type %>" data-path="<%= path %>">' +
                        '<td class="icon"><span class="fa <%= icon %> <%= systemuser %>"></span></td>' +
                        '<td class="name"><a href="#"><%= id %></a></td>' +
                        '<td class="path"><a href="#"><%= path %></a></td>' +
                        '</tr>');
                    if (result.length > 0) {
                        var tab = '<tbody>';
                        result.forEach(function (item) {
                            var type = item.type === 'service' ? 'service' : (item.system ? 'system' : item.type);
                            var icon = type ? usermanagement.const.icon[type] : '';
                            tab += row({
                                type: type,
                                icon: icon ? icon : (item.isGroup ? 'fa-users' : 'fa-user'),
                                systemuser: (item.system ? 'systemuser' : ''),
                                path: item.path,
                                id: item.id
                            });
                        });
                        tab = tab + '</tbody>';
                        this.$resultTable.html(tab);
                        this.$resultTable.find('td a').each(function () {
                            $(this).on('click', _.bind(queryWidget.pathSelected, queryWidget));
                        });
                    } else {
                        this.$resultTable.html('<tbody><tr><td align="center" class="info">no matching nodes found</td></tr></tbody>');
                    }
                }, this), _.bind(function (result) {
                    var message = core.resultMessage(result, 'error on execute query');
                    this.$resultTable.html('<tbody><tr><td class="error danger">' + message + '</td></tr></tbody>');
                }, this));
            },

            pathSelected: function (event) {
                event.preventDefault();
                var $target = $(event.currentTarget);
                var $row = $target.closest('tr');
                var path = $row.attr('data-path');
                $(document).trigger("path:select", [path]);
                return false;
            }

        });

        usermanagement.query = core.getView('#usermanagement-query .query-panel', usermanagement.Query);

    })(CPM.nodes.usermanagement, CPM.namespace('nodes.usermgr.graph'), CPM.core);

})();
