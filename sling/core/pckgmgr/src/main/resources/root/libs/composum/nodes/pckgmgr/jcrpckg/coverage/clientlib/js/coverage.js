/**
 *
 *
 */
(function (core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.CoverageTable = Backbone.View.extend({

            initialize: function (options) {

                this.$table = this.$('.coverage-table');
                this.$table.bootstrapTable({

                    search: true,
                    showToggle: false,
                    striped: true,

                    url: this.$table.data('path'),

                    rowStyle: _.bind(function (row, index) {
                        return {
                            classes: (row.error ? 'error' : 'normal')
                        };
                    }, this),

                    columns: [{
                        class: 'action',
                        field: 'action',
                        title: '',
                        searchable: false,
                        sortable: false,
                        width: '40'
                    }, {
                        class: 'value',
                        field: 'value',
                        title: 'Path / Message',
                        searchable: true,
                        sortable: false,
                        width: '100%'
                    }, {
                        class: 'error',
                        field: 'error',
                        title: '!!',
                        searchable: true,
                        sortable: false,
                        width: '40'
                    }]
                });
            }
        });

        pckgmgr.CoverageTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.table = core.getWidget(this.$el, '.table-container', pckgmgr.CoverageTable);
            }
        });

    })(core.pckgmgr);

})(window.core);
