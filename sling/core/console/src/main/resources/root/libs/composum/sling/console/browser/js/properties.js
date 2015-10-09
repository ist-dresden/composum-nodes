/**
 *
 *
 */
(function(core) {
    'use strict';

    core.browser = core.browser || {};

(function(browser) {

    browser.getPropertiesTab = function() {
        return core.getView('.node-view-panel .properties', browser.PropertiesTab);
    }

    browser.PropertiesTab = browser.NodeTab.extend({

        initialize: function(options) {
            this.table = core.getWidget(this.$el, '.table-container', browser.PropertiesTable);
            this.$addButton = this.$('.table-toolbar .add');
            this.$addButton.click(_.bind (function () {
                browser.openNewPropertyDialog(_.bind (this.reload, this));
            }, this));
            this.$removeButton = this.$('.table-toolbar .remove');
            this.$removeButton.click(_.bind (this.removeSelection, this));
            this.$copyButton = this.$('.table-toolbar .copy');
            this.$copyButton.click(_.bind (this.clipboardCopy, this));
            this.$pasteButton = this.$('.table-toolbar .paste');
            this.$pasteButton.click(_.bind (this.clipboardPaste, this));
        },

        reload: function() {
            this.table.loadContent();
        },

        clipboardCopy: function(event) {
            var selected = this.table.getSelections();
            var path = browser.getCurrentPath();
            var names = [];
            for (var i=0; i < selected.length; i++) {
                names[i] = selected[i].name;
            }
            core.console.getProfile().set('properties', 'clipboard', {
                path: path,
                names: names
            });
        },

        clipboardPaste: function(event) {
            var path = browser.getCurrentPath();
            var clipboard = core.console.getProfile().get('properties', 'clipboard');
            if (path && clipboard && clipboard.path && clipboard.names) {
                $.ajax({
                    url: "/bin/core/property.copy.json" + path,
                    data: JSON.stringify(clipboard),
                    dataType: 'json',
                    type: 'PUT',
                    success: _.bind (function (result) {
                        this.reload();
                    }, this),
                    error: _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error on copying properties', result);
                    }, this)
                });
            }
        },

        removeSelection: function(event) {
            var path = browser.getCurrentPath();
            var selected = this.table.getSelections();
            var names = [];
            for (var i=0; i < selected.length; i++) {
                names[i] = selected[i].name;
            }
            if (path && names) {
                $.ajax({
                    url: "/bin/core/property.remove.json" + path,
                    data: JSON.stringify({ names: names }),
                    dataType: 'json',
                    type: 'DELETE',
                    success: _.bind (function (result) {
                        this.reload();
                    }, this),
                    error: _.bind (function (result) {
                        core.alert ('danger', 'Error', 'Error on removing properties', result);
                    }, this)
                });
            }
        }
    });

    browser.PropertiesTable = Backbone.View.extend({

        initialize: function(options) {

            this.state = {
                load: false,
                editable: false
            };

            this.$table = this.$('.property-table');
            this.$table.bootstrapTable({

                search: true,
                showToggle: false,
                striped: true,

                rowStyle: _.bind (function(row,index) {
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
                    formatter: function(value,row,index) {
                        var escaped = _.escape(value); // prevent from XSS
                        return escaped;
                    }
                }, {
                    class: 'value',
                    field: 'value',
                    title: 'Value',
                    formatter: _.bind (this.formatValue, this),
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

                onClickRow: _.bind (this.onClickRow, this)
            });
        },

        formatValue: function(value,row,index) {
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
                        return '<a href="' + escaped + '">download...</a>';
                    }
                default:
                    return '<a class="editable">' + escaped + '</a>';
            }
            return escaped;
        },

        getSelections: function() {
            var rows = this.$table.bootstrapTable('getSelections');
            return rows;
        },

        onClickRow: function(row, $element) {
            if (!this.state.editable) {
                var type = row.type;
                var $column = $($element.context);
                var columnKey = $column.attr('class');
                if (columnKey == 'value') {
                    var $editable = $element.find('a.editable');
                    if ($editable && $editable.length > 0
                        // if not initialized already - is the case if the editing was canceled
                        && !$editable.hasClass('editable-click')) {
                        var editableType = this.editableTypes[type];
                        var editableOptions = {
                            mode: 'inline',
                            width: '100%',
                            url: this.editableChange
                        }
                        if (editableType) {
                            editableType = row.multi ? editableType.multi : editableType.single;
                            if (editableType) {
                                editableOptions = _.extend (editableOptions, editableType);
                                $column.addClass (editableType.type);
                            }
                        }
                        $editable.editable(editableOptions);
                        $editable.on('shown', _.bind (this.editableShown, this));
                        $editable.on('hidden', _.bind (this.editableHidden, this));
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

        editableShown: function() {
            /* mark editable state 'active' to prevent from parallel dialog activation */
            this.state.editable = true;
        },

        editableHidden: function() {
            /* set inactive for editable with a delay to ensure
               that the dialog is not opened after editable ends */
            setTimeout(_.bind (function(){
                this.state.editable = false;
            }, this), 100);
        },

        editableChange: function(params) {
            var editable = $(this);
            var view = core.getView (editable.closest('.table-container'), browser.PropertiesTable);
            if (! view.state.load) {
                var $row = $(this).closest('tr');
                var property = new browser.Property({
                    path: browser.getCurrentPath(),
                    name: $row.find('td.name').text(),
                    type: $row.find('td.type').text(),
                    multi: ('true' == ($row.find('td.multi').text())),
                    value: params.value
                })
                property.save (_.bind (function (result) {
                        view.loadContent(event);
                    }, view), _.bind (function (result) {
                        if (result.status != 200) {
                            core.alert ('danger', 'Error', 'Error on updating properties', result);
                        }
                        view.loadContent();
                    }, view));
            }
        },

        openEditDialog: function(row,element) {
            if (! this.state.editable) {
                var dialog = core.browser.getPropertyDialog();
                dialog.show(_.bind (function(){
                    if (row) {
                        dialog.setProperty(
                            new browser.Property({
                                path: browser.getCurrentPath(),
                                name: row.name,
                                type: row.type,
                                multi: row.multi,
                                value: row.value
                            })
                        );
                    } else {
                        dialog.setProperty(
                            new browser.Property({
                                path: browser.getCurrentPath()
                            })
                        );
                    }
                }, this),
                _.bind (this.loadContent, this));
            }
        },

        loadContent: function() {
            var path = browser.getCurrentPath();
            this.state.load = true;
            $.ajax({
                url: "/bin/core/property.map.json" + path,
                dataType: 'json',
                type: 'GET',
                success: _.bind (function (result) {
                    this.$table.bootstrapTable('load', result);
                }, this),
                error: _.bind (function (result) {
                    core.alert ('danger', 'Error', 'Error on loading properties', result);
                }, this),
                complete: _.bind (function (result) {
                    this.state.load = false;
                }, this)
            });
        }
    });

})(core.browser);

})(window.core);
