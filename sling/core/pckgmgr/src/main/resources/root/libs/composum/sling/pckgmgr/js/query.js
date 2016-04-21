(function (core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.Query = Backbone.View.extend({

            initialize: function (options) {
                this.$form = this.$('.query-actions form');
                this.$queryInput = this.$('.query-actions form input');
                this.$execButton = this.$('.query-actions .exec');
                this.$resultTable = this.$('.query-result table tbody');
                this.$form.on('submit', _.bind(this.executeQuery, this));
                this.$execButton.on('click.query', _.bind(this.executeQuery, this));
            },

            executeQuery: function (event) {
                event.preventDefault();
                this.$resultTable.html('<tr><td class="pulse" colspan="5"><i class="fa fa-spinner fa-pulse"></i></td></tr>');
                var query = this.$queryInput.val();
                core.ajaxGet('/bin/core/package.query.json/' + encodeURIComponent(query), {
                    dataType: 'json'
                }, _.bind(function (result) {
                    var queryWidget = this;
                    var rowTemplate = _.template(
                        '<tr data-path="<%= path %>">'
                        + '<td class="state">'
                        + '<span class="installed-<%= state.installed %>">I</span>'
                        + '<span class="sealed-<%= state.sealed %>">S</span>'
                        + '<span class="valid-<%= state.valid %>">V</span>'
                        + '</td>'
                        + '<td class="group"><a href="#"><%= group %></a></td>'
                        + '<td class="name"><a href="#"><%= name %></a></td>'
                        + '<td class="version"><a href="#"><%= version %></a></td>'
                        + '<td class="date last-modified"><a href="#"><%= lastModified %></a></td>'
                        + '</tr>');
                    if (result.length > 0) {
                        var tableContent = '';
                        result.forEach(function (row) {
                            tableContent += rowTemplate(row);
                        });
                        this.$resultTable.html(tableContent);
                        this.$resultTable.find('td a').on('click', _.bind(queryWidget.pathSelected, queryWidget));
                    } else {
                        this.$resultTable.html('<tr><td align="center" class="info" colspan="5">no matching packages found</td></tr>');
                    }
                }, this), _.bind(function (result) {
                    var message = core.resultMessage(result, 'error on execute query');
                    this.$resultTable.html('<tr><td class="error danger" colspan="5">' + message + '</td></tr>');
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

        pckgmgr.query = core.getView('#pckgmgr-query .query-panel', pckgmgr.Query);

    })(core.pckgmgr);

})(window.core);
