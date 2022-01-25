(function () {
    'use strict';
    CPM.namespace('nodes.tools.bundles');

    (function (bundles, core) {

        bundles.View = Backbone.View.extend({

            initialize: function () {
                this.$title = $('.tools-osgi-bundles_header h1');
                this.$total = this.$title.find('.total');
                this.$active = this.$title.find('.active');
                this.$status = this.$title.find('i');
                this.$table = $('.tools-osgi-bundles_table');
                this.$table.bootstrapTable({

                    search: true,
                    showToggle: false,
                    striped: true,

                    onClickRow: _.bind(function (row) {
                        this.openDetails(row.id);
                    }, this),

                    rowStyle: _.bind(function (row, index) {
                        return {
                            classes: 'state-' + row.state + (row.active ? '' : ' attention')
                        };
                    }, this),

                    rowAttributes: _.bind(function (row, index) {
                        return {
                            'data-bundle-id': row.id
                        };
                    }, this),

                    columns: [{
                        class: 'id',
                        field: 'id',
                        title: 'Id',
                        checkbox: false,
                        sortable: true
                    }, {
                        class: 'name',
                        field: 'name',
                        title: 'Name',
                        searchable: true,
                        sortable: true,
                        width: '100%',
                        formatter: _.bind(function (value, row, index) {
                            return value + ' <span class="symbolic">(' + row.symbolicName + ')</span>';
                        }, this)
                    }, {
                        class: 'version',
                        field: 'version',
                        title: 'Version',
                        searchable: false,
                        sortable: false
                    }, {
                        class: 'category',
                        field: 'category',
                        title: 'Category',
                        searchable: true,
                        sortable: true
                    }, {
                        class: 'state',
                        field: 'state',
                        title: 'State',
                        searchable: false,
                        sortable: true
                    }, {
                        class: 'modified',
                        field: 'lastModified',
                        title: 'last modified',
                        searchable: false,
                        sortable: true
                    }]
                });
                this.loadContent();
            },

            loadContent: function () {
                core.getJson(core.getComposumPath('composum/nodes/system/tools/osgi/bundles.json'),
                    _.bind(function (result) {
                        this.$table.bootstrapTable('load', result.bundles);
                        this.$total.text('total: ' + result.total);
                        this.$active.text('active: ' + result.active);
                        if (result.active < result.total) {
                            this.$title.addClass('attention');
                            this.$status.removeClass().addClass('fa fa-exclamation-triangle');
                        } else {
                            this.$title.removeClass('attention');
                            this.$status.removeClass().addClass('fa fa-check-square');
                        }
                    }, this), _.bind(function (result) {
                        if (result.status === 404) {
                            this.$table.bootstrapTable('load', []);
                        } else {
                            core.alert('danger', 'Error', 'Error on loading bundles', result);
                        }
                    }, this));
            },

            openDetails: function (bundleId) {
                core.getJson(core.getComposumPath('composum/nodes/system/tools/osgi/bundles.json/') + bundleId,
                    _.bind(function (bundle) {
                        core.showLoadedDialog(core.components.Dialog,
                            this.renderDialog(bundle, this.renderDetails(bundle)));
                    }, this));
            },

            renderDialog: function (bundle, content) {
                return '<div class="tools-osgi-bundle_popup modal dialog fade">' +
                    '<div class="tools-osgi-bundle_modal modal-dialog modal-lg"><div class="modal-content">' +
                    '<div class="modal-header"><button type="button" class="close" data-dismiss="modal"'
                    + ' aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
                    '<h4 class="modal-title">' + bundle.id + ' - ' + bundle.name
                    + ' <span class="symbolic">(' + bundle.symbolicName + ')</span></h4></div>' +
                    '<div class="modal-body">' + content + '</div>' +
                    '<div class="modal-footer"><button type="button" class="btn btn-default"'
                    + ' data-dismiss="modal">Close</button></div>' +
                    '</div></div></div>';
            },

            renderDetails: function (bundle) {
                return '<div class="tools-osgi-bundle_details">' +
                    '<ul class="nav nav-tabs" role="tablist">' +
                    this.renderTabHead(bundle, 'summary', 'Summary', true) +
                    this.renderTabHead(bundle, 'exported', 'Exported') +
                    this.renderTabHead(bundle, 'imported', 'Imported') +
                    this.renderTabHead(bundle, 'services', 'Provided') +
                    this.renderTabHead(bundle, 'used', 'Used') +
                    this.renderTabHead(bundle, 'headers', 'Headers') +
                    '</ul><div class="tab-content">' +
                    this.renderTabBody(bundle, 'summary', this.renderBundleTab(bundle), true) +
                    this.renderTabBody(bundle, 'exported', this.renderExportedTab(bundle)) +
                    this.renderTabBody(bundle, 'imported', this.renderImportedTab(bundle)) +
                    this.renderTabBody(bundle, 'services', this.renderProvidedTab(bundle)) +
                    this.renderTabBody(bundle, 'used', this.renderUsedTab(bundle)) +
                    this.renderTabBody(bundle, 'headers', this.renderHeadersTab(bundle)) +
                    '</div></div>';
            },

            renderBundleTab: function (bundle) {
                var imported = this.countPackages(bundle.imported);
                return this.renderDetailsTable(bundle, this.renderDetailsTableRows({
                    'Bundle Name': bundle.name,
                    'Symbolic Name': bundle.symbolicName,
                    'Version': bundle.version,
                    'Last Modified': bundle.lastModified,
                    'Location': bundle.location
                }) + this.renderDetailsTableRows(bundle.more) + this.renderDetailsTableRows({
                    'State': (bundle.active ? 'O@' : (bundle.state === 'resolved' ? 'R@' : 'A@')) + bundle.state,
                    'Exported': bundle.exported.length + ' packages',
                    'Imported': (imported.missed ? 'A@' : (imported.resolved ? 'R@' : ''))
                        + imported.total + ' packages; wired: ' + imported.active
                        + (imported.resolved > 0 ? ', resolved: ' + imported.resolved : '')
                        + (imported.missed > 0 ? ', not resolvable: ' + imported.missed : ''),
                    'Provided': bundle.provided.length + ' services',
                    'Used': bundle.used.length + ' services'
                }));
            },

            renderExportedTab: function (bundle) {
                return this.renderDetailsTable(bundle,
                    this.renderDetailsTableRows(bundle.exported, function (item) {
                        return {
                            text: item.symbolicName + ' (' + item.version + ')'
                        };
                    }));
            },

            renderImportedTab: function (bundle) {
                return this.renderDetailsTable(bundle,
                    this.renderDetailsTableRows(bundle.imported, function (item) {
                        return {
                            text: item.symbolicName + (item.range ? (' ' + item.range) : '')
                                + (item.version ? ' :: ' + item.version + ' (' + item.bundle + ')' : '')
                                + (item.optional ? '; optional' : ''),
                            rowCss: item.resolved ? (item.active ? '' : 'resolved')
                                : (item.optional ? 'resolved' : 'attention')
                        };
                    }));
            },

            countPackages: function (packages) {
                var result = {total: 0, active: 0, resolved: 0, missed: 0};
                packages.forEach(_.bind(function (pckge) {
                    result.total++;
                    if (pckge.active) {
                        result.active++;
                    } else {
                        if (pckge.resolved) {
                            result.resolved++;
                        } else {
                            result.missed++;
                        }
                    }
                }, this));
                return result;
            },

            renderProvidedTab: function (bundle) {
                return this.renderServicesTab(bundle, bundle.provided);
            },

            renderUsedTab: function (bundle) {
                return this.renderServicesTab(bundle, bundle.used);
            },

            renderServicesTab: function (bundle, services) {
                var content = '';
                var i;
                services.forEach(_.bind(function (service) {
                    var left = '<div class="id">' + service.id + '</div>';
                    for (i = 0; i < service.short.length; i++) {
                        left += '<div class="type">' + service.short[i] + '</div>';
                    }
                    var right = '';
                    if (service.description) {
                        right += '<div class="description">' + service.description + '</div>';
                    }
                    right += '<ul class="classes">';
                    for (i = 0; i < service.classes.length; i++) {
                        right += '<li class="type">' + service.classes[i] + '</li>';
                    }
                    right += '</ul>';
                    if (service.component) {
                        right += '<div class="component">component: ' + service.component.id
                            + ' (' + service.component.name + ')</div>';
                    }
                    if (service.servicePid) {
                        right += '<div class="service-pid">servicePid: ' + service.servicePid;
                    }
                    for (var prop in service.properties) {
                        right += '<div class="property"><span class="key">' + prop + '</span>: <span class="value">'
                            + service.properties[prop] + '</span></div>';
                    }
                    content += this.renderDetailsTableRow(left, right);
                }, this));
                return this.renderDetailsTable(bundle, content);
            },

            renderHeadersTab: function (bundle) {
                return this.renderDetailsTable(bundle, this.renderDetailsTableRows(bundle.headers));
            },

            renderTabHead: function (bundle, key, title, active) {
                var id = 'bundle-' + bundle.id + '-' + key
                return '<li role="presentation" class="'
                    + (active ? 'active' : '') + '"><a role="tab" data-toggle="tab"'
                    + ' href="#' + id + '" aria-controls="' + id + '">' + title + '</a></li>';
            },

            renderTabBody: function (bundle, key, content, active) {
                var id = 'bundle-' + bundle.id + '-' + key
                return '<div id="' + id + '" class="tools-osgi-bundle_details-tab tab-pane fade'
                    + (active ? ' in active' : '') + '" role="tabpanel">' +
                    '<div class="tools-osgi-bundle_details-tab-content">' + content + '</div></div>';
            },

            renderDetailsTable: function (bundle, content) {
                return '<table class="tools-osgi-bundle_details-table">' + content + '</table>';
            },

            renderDetailsTableRows: function (object, fmt) {
                var result = "";
                if (_.isArray(object)) {
                    object.forEach(_.bind(function (item) {
                        var value = _.isFunction(fmt) ? fmt(item) : {text: item};
                        result += this.renderDetailsTableRow(undefined, value.text, value.rowCss);
                    }, this));
                } else {
                    for (var prop in object) {
                        if (object[prop]) {
                            var value = _.isFunction(fmt) ? fmt(object[prop]) : {text: object[prop]};
                            result += this.renderDetailsTableRow(prop, value.text, value.rowCss);
                        }
                    }
                }
                return result;
            },

            renderDetailsTableRow: function (name, value, rowCss) {
                if (!rowCss) {
                    if (value.indexOf('A@') === 0) {
                        value = value.substring(2);
                        rowCss = 'attention';
                    } else if (value.indexOf('R@') === 0) {
                        value = value.substring(2);
                        rowCss = 'resolved';
                    } else if (value.indexOf('O@') === 0) {
                        value = value.substring(2);
                        rowCss = 'success';
                    }
                }
                return name
                    ? ('<tr class="tools-osgi-bundle_details-table_row' + (rowCss ? (' ' + rowCss) : '') + '"><td'
                        + ' class="tools-osgi-bundle_details-table_name">' + name + '</td><td'
                        + ' class="tools-osgi-bundle_details-table_value">' + value + '</td>' +
                        '</tr>')
                    : ('<tr class="tools-osgi-bundle_details-table_row' + (rowCss ? (' ' + rowCss) : '') + '"><td'
                        + ' class="tools-osgi-bundle_details-table_value" colspan="2">' + value + '</td>' +
                        '</tr>');
            }
        });

        bundles.view = core.getView('#tools-osgi-bundles', bundles.View);

    })(CPM.nodes.tools.bundles, CPM.core);
})();
