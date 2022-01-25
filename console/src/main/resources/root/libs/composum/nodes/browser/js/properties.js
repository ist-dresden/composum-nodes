/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.browser');

    (function (browser, core) {

        browser.getPropertiesTab = function () {
            return core.getView('.node-view-panel .properties', browser.PropertiesTab);
        };

        browser.PropertiesTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.table = core.getWidget(this.$el, '.table-container', browser.PropertiesTable, _.extend({
                    path: this.getPath,
                    selectPath: this.selectPath,
                }, options));
                this.$addButton = this.$('.table-toolbar .add');
                this.$addButton.click(_.bind(function (event) {
                    event.preventDefault();
                    browser.openNewPropertyDialog(_.bind(function () {
                        this.reload();
                    }, this), this.getPath());
                    return false;
                }, this));
                this.$removeButton = this.$('.table-toolbar .remove');
                this.$removeButton.click(_.bind(this.removeSelection, this));
                this.$copyButton = this.$('.table-toolbar .copy');
                this.$copyButton.click(_.bind(this.clipboardCopy, this));
                this.$pasteButton = this.$('.table-toolbar .paste');
                this.$pasteButton.click(_.bind(this.clipboardPaste, this));
                this.$('.table-toolbar .reload').click(_.bind(this.reload, this));
            },

            getPath: function () {
                return browser.getCurrentPath();
            },

            selectPath: function (path) {
                if (path) {
                    browser.setCurrentPath(path);
                }
            },

            reload: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.table.loadContent();
                return false;
            },

            propertyChanged: function (path) {
                $(document).trigger('path:changed', [path]);
            },

            clipboardCopy: function (event) {
                event.preventDefault();
                var path = this.getPath();
                if (path) {
                    var selected = this.table.getSelections();
                    var names = [];
                    for (var i = 0; i < selected.length; i++) {
                        names[i] = selected[i].name;
                    }
                    core.console.getProfile().set('properties', 'clipboard', {
                        path: path,
                        names: names
                    });
                }
                return false;
            },

            clipboardPaste: function (event) {
                event.preventDefault();
                var path = this.getPath();
                if (path) {
                    var clipboard = core.console.getProfile().get('properties', 'clipboard');
                    if (clipboard && clipboard.path && clipboard.names) {
                        core.ajaxPut("/bin/cpm/nodes/property.copy.json" + core.encodePath(path),
                            JSON.stringify(clipboard), {
                                dataType: 'json'
                            }, _.bind(function (result) {
                                this.propertyChanged(path);
                            }, this), _.bind(function (result) {
                                core.alert('danger', 'Error', 'Error on copying properties', result);
                            }, this));
                    }
                }
                return false;
            },

            removeSelection: function (event) {
                event.preventDefault();
                var path = this.getPath();
                if (path) {
                    var selected = this.table.getSelections();
                    var names = [];
                    for (var i = 0; i < selected.length; i++) {
                        names[i] = selected[i].name;
                    }
                    if (names) {
                        core.ajaxDelete("/bin/cpm/nodes/property.remove.json" + core.encodePath(path), {
                            data: JSON.stringify({names: names}),
                            dataType: 'json'
                        }, _.bind(function (result) {
                            this.propertyChanged(path);
                        }, this), _.bind(function (result) {
                            core.alert('danger', 'Error', 'Error on removing properties', result);
                        }, this));
                    }
                }
                return false;
            }
        });

        browser.PropertiesTable = Backbone.View.extend({

            initialize: function (options) {

                this.permission = this.$el.data('permission');
                this.path = options.path || this.$el.data('path');
                this.selectPath = options.selectPath;

                this.state = {
                    load: false,
                    editable: false
                };

                this.$table = this.$('.property-table');
                this.$table.bootstrapTable({

                    search: options.search !== undefined ? options.search : true,
                    showToggle: false,
                    striped: true,

                    rowStyle: _.bind(function (row, index) {
                        return {
                            classes: (row.protected ? 'protected' : 'editable')
                        };
                    }, this),

                    columns: [{
                        class: 'selection',
                        checkbox: true,
                        sortable: false
                    }, {
                        class: 'name',
                        field: 'name',
                        title: 'Name',
                        searchable: true,
                        sortable: true,
                        width: '22%',
                        formatter: function (value, row, index) {
                            return _.escape(value); // prevent from XSS
                        }
                    }, {
                        class: 'value',
                        field: 'value',
                        title: 'Value',
                        formatter: _.bind(this.formatValue, this),
                        searchable: true,
                        sortable: false,
                        width: '68%'
                    }, {
                        class: 'multi',
                        field: 'multi',
                        title: 'Multi',
                        searchable: false,
                        sortable: false,
                        width: '50px'
                    }, {
                        class: 'type',
                        field: 'type',
                        title: 'Type',
                        searchable: false,
                        sortable: false,
                        width: '150px'
                    }, {
                        class: 'auto',
                        field: 'auto',
                        title: 'Auto',
                        searchable: false,
                        sortable: false,
                        width: '50px'
                    }],

                    onClickRow: _.bind(this.onClickRow, this),
                    onPostBody: _.bind(this.onLoad, this)
                });
            },

            getPath: function () {
                return _.isFunction(this.path) ? this.path() : this.path;
            },

            formatValue: function (value, row, index) {
                var type = row.type;
                if (_.isArray(value)) {
                    value = value.join(', ');
                }
                var escaped = _.escape(value); // prevent from XSS
                switch (type) {
                    case 'Binary':
                        if (row.multi) {
                            return '<a class="editable">' + escaped + '</a>';
                        } else {
                            escaped = core.getContextUrl(escaped);
                            return '<a href="' + escaped + '">download...</a>';
                        }
                    default:
                        var text = '<a class="editable">' + escaped;
                        if (row.target) {
                            text += '<span class="target-link btn btn-sm btn-default fa fa-share"'
                                + ' data-path="' + row.target + '"' + ' title="' + row.target + '"></span>'
                        }
                        text += '</a>'
                        return text;
                }
            },

            onLoad: function () {
                this.$table.find('td .target-link').click(_.bind(function (event) {
                    event.preventDefault();
                    var $link = $(event.currentTarget);
                    var path = $link.data('path');
                    if (path) {
                        if (_.isFunction(this.selectPath)) {
                            this.selectPath(path);
                        }
                    }
                    return false;
                }, this));
            },

            getSelections: function () {
                return this.$table.bootstrapTable('getSelections');
            },

            onClickRow: function (row, $element) {
                if (!this.state.editable) {
                    var type = row.type;
                    var $column = $($element.context);
                    var columnKey = $column.attr('class');
                    if (columnKey === 'value') {
                        var $editable = $element.find('a.editable');
                        if ($editable && $editable.length > 0 && this.permission === 'write'
                            // if not initialized already - is the case if the editing was canceled
                            && !$editable.hasClass('editable-click')) {
                            var editableType = this.editableTypes[type];
                            var editableOptions = {
                                mode: 'inline',
                                width: '100%',
                                url: this.editableChange
                            };
                            if (editableType) {
                                editableType = row.multi ? editableType.multi : editableType.single;
                                if (editableType) {
                                    editableOptions = _.extend(editableOptions, editableType);
                                    $column.addClass(editableType.type);
                                }
                            }
                            $editable.editable(editableOptions);
                            $editable.on('shown', _.bind(this.editableShown, this));
                            $editable.on('hidden', _.bind(this.editableHidden, this));
                            $editable.editable('toggle');
                        }
                    } else {
                        this.openEditDialog(row, $element);
                    }
                }
            },

            editableTypes: {
                'Boolean': {
                    single: {
                        type: 'checkbox'
                    }
                }
            },

            editableShown: function () {
                /* mark editable state 'active' to prevent from parallel dialog activation */
                this.state.editable = true;
            },

            editableHidden: function () {
                /* set inactive for editable with a delay to ensure
                 that the dialog is not opened after editable ends */
                setTimeout(_.bind(function () {
                    this.state.editable = false;
                }, this), 100);
            },

            editableChange: function (params) {
                var editable = $(this);
                var view = core.getView(editable.closest('.table-container'), browser.PropertiesTable);
                if (!view.state.load) {
                    var path = view.getPath();
                    if (path) {
                        var $row = $(this).closest('tr');
                        var property = new browser.Property({
                            path: path,
                            name: $row.find('td.name').text(),
                            oldname: $row.find('td.name').text(),
                            type: $row.find('td.type').text(),
                            multi: ('true' === ($row.find('td.multi').text())),
                            value: params.value
                        });
                        property.save(_.bind(function (result) {
                            $(document).trigger('path:changed', [path]);
                        }, view), _.bind(function (result) {
                            if (result.status !== 200) {
                                core.alert('danger', 'Error', 'Error on updating properties', result);
                            }
                            view.loadContent();
                        }, view));
                    }
                }
            },

            openEditDialog: function (row, element) {
                if (!this.state.editable) {
                    var path = this.getPath();
                    if (path) {
                        var dialog = browser.getPropertyDialog(path);
                        dialog.show(_.bind(function () {
                                if (row) {
                                    dialog.setProperty(
                                        new browser.Property({
                                            path: path,
                                            name: row.name,
                                            type: row.type,
                                            subtype: row.subtype,
                                            multi: row.multi,
                                            value: row.value
                                        })
                                    );
                                } else {
                                    dialog.setProperty(
                                        new browser.Property({
                                            path: path
                                        })
                                    );
                                }
                                if (this.permission !== 'write') {
                                    dialog.readonly();
                                }
                            }, this),
                            _.bind(this.loadContent, this));
                    }
                }
            },

            loadContent: function () {
                var path = this.getPath();
                if (path) {
                    this.state.load = true;
                    core.getJson("/bin/cpm/nodes/property.map.json" + core.encodePath(path),
                        _.bind(function (result) {
                            this.$table.bootstrapTable('load', result);
                        }, this), _.bind(function (result) {
                            if (result.status === 404) {
                                this.$table.bootstrapTable('load', []);
                            } else {
                                core.alert('danger', 'Error', 'Error on loading properties', result);
                            }
                        }, this), _.bind(function (result) {
                            this.state.load = false;
                        }, this));
                } else {
                    this.$table.bootstrapTable('load', []);
                }
            }
        });

    })(CPM.nodes.browser, CPM.core);

})();
