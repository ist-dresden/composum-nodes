(function () {
    'use strict';
    CPM.namespace('nodes.tools.settings');

    (function (settings, core) {

        settings.profileId = 'system-settings';

        settings.View = Backbone.View.extend({

            initialize: function () {
                this.$title = $('.tools-runtime-settings_header h1');
                this.$tabs = $('.tools-runtime-settings_tabs');
                this.$table = $('.tools-runtime-settings_table');
                this.$table.bootstrapTable({

                    search: true,
                    showToggle: false,
                    striped: true,

                    columns: [{
                        class: 'key',
                        field: 'key',
                        title: 'Key',
                        searchable: true,
                        sortable: true
                    }, {
                        class: 'value',
                        field: 'value',
                        title: 'Value',
                        searchable: true,
                        sortable: false,
                        width: '100%'
                    }]
                });
                this.$tabs.find('a').click(_.bind(this.selectTab, this));
                this.currentTab = core.console.getProfile().get(settings.profileId, 'current', 'sling');
                this.selectTab();
            },

            selectTab: function (event) {
                if (event) {
                    event.preventDefault();
                    var $tab = $(event.currentTarget);
                    this.currentTab = $tab.data('selector');
                    core.console.getProfile().set(settings.profileId, 'current', this.currentTab);
                }
                this.$tabs.find('a').removeClass('active');
                this.$tabs.find('a[data-selector="' + this.currentTab + '"]').addClass('active');
                this.loadContent();
            },

            loadContent: function () {
                core.getJson(core.getComposumPath('composum/nodes/system/tools/runtime/settings.' + (this.currentTab) + '.json'),
                    _.bind(function (result) {
                        this.$table.bootstrapTable('load', result);
                    }, this), _.bind(function (result) {
                        if (result.status === 404) {
                            this.$table.bootstrapTable('load', []);
                        } else {
                            core.alert('danger', 'Error', 'Error on loading settings', result);
                        }
                    }, this));
            }
        });

        settings.view = core.getView('#tools-runtime-settings', settings.View);

    })(CPM.nodes.tools.settings, CPM.core);
})();
