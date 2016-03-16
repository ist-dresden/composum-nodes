(function (core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function (usermanagement) {


        usermanagement.Query = Backbone.View.extend({

            initialize: function (options) {
                this.$form = this.$('.query-actions form');
                this.$queryInput = this.$('.query-actions form input');
                this.$execButton = this.$('.query-actions .exec');
                this.$resultTable = this.$('.query-result table');
                this.$form.on('submit', _.bind(this.executeQuery, this));
                this.$execButton.on('click.query', _.bind(this.executeQuery, this));
            },

            executeQuery: function (event) {
                event.preventDefault();
                this.$resultTable.html('<tbody><tr><td class="pulse"><i class="fa fa-spinner fa-pulse"></i></td></tr></tbody>');
                var query = this.$queryInput.val();
                core.ajaxGet('/bin/core/usermanagement.query.json/' + query, {
                    dataType: 'json'
                }, _.bind(function (result) {
                    var queryWidget = this;
                    var row = _.template(
                        '<tr data-path="<%= path %>">' +
                            '<td class="icon"><span class="fa <%= icon %> <%= systemuser %>"></span></td>' +
                            '<td class="name"><%= id %></td>' +
                            '<td class="path"><a href="<%= path %>"><%= path %></a></td>' +
                        '</tr>');
                    if (result.length > 0) {
                        var tab = '<tbody>';
                        result.forEach(function(r) {
                            tab += row({
                                icon: (r.isGroup ? 'fa-users' : 'fa-user'),
                                systemuser: (r.systemUser ? 'systemuser' : ''),
                                path: r.path,
                                id: r.id
                            });
                        });
                        tab = tab + '</tbody>';
                        this.$resultTable.html(tab);
                        this.$resultTable.find('td.path').each(function () {
                            var $td = $(this);
                            $td.find('a').on('click', _.bind(queryWidget.pathSelected, queryWidget));
                        });

                    } else {
                        this.$resultTable.html('<tbody><tr><td align="center" class="info">no matching nodes found</td></tr></tbody>');
                    }
                }, this), _.bind(function (result) {
                    var message = core.resultMessage(result, 'error on execute query');
                    this.$resultTable.html('<tbody><tr><td class="error danger">' + message + '</td></tr></tbody>');
                }, this));
                return false;
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

    })(core.usermanagement);

})(window.core);
