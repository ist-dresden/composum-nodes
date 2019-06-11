/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.PoliciesTable = Backbone.View.extend({

            initialize: function (options) {

                var columns = [{
                    visible: false,
                    field: 'index',
                    title: 'index',
                    class: 'index'
                }, {
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
                    options.singleSelect = true;
                    options.clickToSelect = true;
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
            },

            getData: function (idx) {
                var rows = this.$el.bootstrapTable('getData');
                return _.find(rows, function (row) {
                    return (row.index == idx);
                })
            },

            check: function (idx) {
                this.$el.bootstrapTable('check', idx);
            },

            numberOfRows: function () {
                return this.$el.bootstrapTable('getData').length;
            }
        });

    })(core.browser);

})(window.core);
