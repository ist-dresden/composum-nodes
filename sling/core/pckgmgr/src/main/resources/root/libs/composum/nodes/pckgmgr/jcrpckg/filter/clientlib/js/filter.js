/**
 *
 *
 */
(function (core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        /**
         * the generic 'property-value-widget' derived from the 'multi-value-widget'
         */
        pckgmgr.FilterRulesItem = core.components.MultiFormItem.extend({

            initialize: function (options) {
                core.components.MultiFormItem.prototype.initialize.apply(this, [options]);
                this.type = core.getWidget(this.$el, '.type', core.components.SelectWidget);
                this.pattern = core.getWidget(this.$el, '.pattern', core.components.TextFieldWidget);
            },

            reset: function () {
                this.type.setValue('include');
                this.pattern.setValue('');
            }
        });

        /**
         * the generic 'property-value-widget' derived from the 'multi-value-widget'
         */
        pckgmgr.FilterRulesWidget = core.components.MultiFormWidget.extend({

            initialize: function (options) {
                options = _.extend({itemType: pckgmgr.FilterRulesItem}, options);
                core.components.MultiFormWidget.prototype.initialize.apply(this, [options]);
            },

            getRules: function () {
                var rules = [];
                for (var i = 0; i < this.itemList.length; i++) {
                    var type = this.itemList[i].type.getValue();
                    var pattern = this.itemList[i].pattern.getValue();
                    if (type && pattern) {
                        rules.push({type: type, pattern: pattern});
                    }
                }
                return rules;
            },

            setRules: function (rules) {
                if (rules) {
                    this.reset(rules.length);
                    for (var i = 0; i < this.itemList.length; i++) {
                        this.itemList[i].type.setValue(rules[i].type);
                        this.itemList[i].pattern.setValue(rules[i].pattern);
                    }
                } else {
                    this.reset(0);
                }
            }
        });

        pckgmgr.EditFilterDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$title = this.$('.modal-title');
                this.$index = this.$('input[name="index"]');
                this.$root = this.$('input[name="root"]');
                this.$importMode = this.$('select[name="importMode"]');
                this.filters = core.getWidget(this.$el, '.widget.filter-rules-widget', pckgmgr.FilterRulesWidget);
                this.$delete = this.$('button.delete');
                this.$create = this.$('button.create');
                this.$save = this.$('button.save');
                this.$delete.click(_.bind(this.deleteFilter, this));
                this.$create.click(_.bind(this.submitFilter, this));
                this.$save.click(_.bind(this.submitFilter, this));
            },

            setFilter: function (index, row) {
                this.$index.val(index >= 0 ? index : -1);
                if (row) {
                    this.form.$el.attr('action', core.getContextUrl('/bin/cpm/package.filterChange.html' + pckgmgr.getCurrentPath()));
                    this.form.$el.removeClass('create').addClass('change');
                    this.$title.text('Change Package Filter');
                    this.$root.val(row.root);
                    this.$importMode.val(row.importMode);
                    this.filters.setRules(row.rules);
                } else {
                    this.form.$el.attr('action', core.getContextUrl('/bin/cpm/package.filterAdd.html' + pckgmgr.getCurrentPath()));
                    this.form.$el.removeClass('change').addClass('create');
                    this.$title.text('Create Package Filter');
                    this.$root.val('');
                    this.$importMode.val(undefined);
                    this.filters.setRules();
                }
            },

            submitFilter: function (event) {
                event.preventDefault();
                if (this.form.isValid()) {
                    this.submitForm();
                } else {
                    this.alert('danger', 'a root path must be specified');
                }
                return false;
            },

            deleteFilter: function (event) {
                event.preventDefault();
                core.ajaxPost('/bin/cpm/package.filterRemove.html' + pckgmgr.getCurrentPath(), {
                    index: this.$index.val()
                }, {}, _.bind(function () {
                    this.hide();
                }, this), _.bind(function (result) {
                    this.alert('danger', "Error on delete", result);
                }, this));
                return false;
            }
        });

        pckgmgr.getEditFilterDialog = function () {
            return core.getView('#pckg-filter-dialog', pckgmgr.EditFilterDialog);
        };

        pckgmgr.openEditFilterDialog = function (index, row, callback) {
            var dialog = pckgmgr.getEditFilterDialog();
            dialog.show(_.bind(function () {
                dialog.setFilter(index, row);
            }, this), callback);
        };

        pckgmgr.FiltersTable = Backbone.View.extend({

            initialize: function (options) {

                this.$table = this.$('.filters-table');
                this.$table.bootstrapTable({

                    search: true,
                    showToggle: false,
                    striped: true,
                    clickToSelect: true,
                    singleSelect: true,

                    url: this.$table.data('path'),

                    columns: [{
                        class: 'selection',
                        checkbox: true,
                        sortable: false
                    }, {
                        class: 'root',
                        field: 'root',
                        title: 'Root Path / Patterns',
                        searchable: true,
                        sortable: true,
                        width: '100%',
                        formatter: this.formatter
                    }]
                });
            },

            formatter: function (value, row, index) {
                row.index = index;
                var result = '<div class="filter-rule" data-index="' + index + '">';
                result += '<div class="root-path">';
                result += value;
                result += '</div>';
                if (row.rules) {
                    result += '<ul class="filter-rules">';
                    for (var i = 0; i < row.rules.length; i++) {
                        result += '<li class="rule ' + row.rules[i].type + '">';
                        result += '<span class="type">' + row.rules[i].type + '</span>';
                        result += '<span class="pattern">' + row.rules[i].pattern + '</span>';
                        result += '</li>'
                    }
                    result += '</ul>'
                }
                result += '</div>';
                return result;
            },

            getRowCount: function () {
                return this.$table.bootstrapTable('getData').length;
            },

            getSelections: function () {
                var rows = this.$table.bootstrapTable('getSelections');
                return rows;
            },

            setSelection: function (index) {
                if (index >= 0 && index < this.getRowCount()) {
                    this.$table.bootstrapTable('check', index);
                } else {
                    this.$table.bootstrapTable('uncheckAll');
                }
            },

            refresh: function (index) {
                if (index >= 0) {
                    this.$table.on('load-success.bs.table.selection', _.bind(function () {
                        this.$table.off('load-success.bs.table.selection');
                        this.setSelection(index);
                    }, this));
                }
                this.$table.bootstrapTable('refresh');
            }
        });

        pckgmgr.FiltersTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.table = core.getWidget(this.$el, '.table-container', pckgmgr.FiltersTable);
                this.$edit = this.$('.table-toolbar .edit');
                this.$edit.click(_.bind(this.editFilter, this));
                this.$('.table-toolbar .add').click(_.bind(this.createFilter, this));
                this.$('.table-toolbar .remove').click(_.bind(this.deleteFilter, this));
                this.$('.table-toolbar .move-up').click(_.bind(this.moveFilterUp, this));
                this.$('.table-toolbar .move-down').click(_.bind(this.moveFilterDown, this));
                this.$('.table-toolbar .reload').click(_.bind(this.refresh, this));
            },

            editFilter: function (event) {
                event.preventDefault();
                var row = this.getSelectedRow();
                if (row) {
                    pckgmgr.openEditFilterDialog(this.currentIndex, row, _.bind(this.refresh, this));
                }
            },

            createFilter: function (event) {
                event.preventDefault();
                this.getSelectedRow();
                pckgmgr.openEditFilterDialog(this.currentIndex, undefined, _.bind(this.refresh, this));
            },

            deleteFilter: function (event) {
                event.preventDefault();
                var row = this.getSelectedRow();
                if (row) {
                    core.ajaxPost('/bin/cpm/package.filterRemove.html' + pckgmgr.getCurrentPath(), {
                        index: row.index
                    }, {}, _.bind(function () {
                        this.refresh();
                    }, this), _.bind(function (result) {
                        core.alert('danger', "Error", "Error on filter delete", result);
                    }, this));
                }
            },

            moveFilterUp: function (event) {
                this.moveFilter(event, 'Up', -1);
            },

            moveFilterDown: function (event) {
                this.moveFilter(event, 'Down', 1);
            },

            moveFilter: function (event, dir, indexInc) {
                event.preventDefault();
                var row = this.getSelectedRow();
                if (row) {
                    core.ajaxPost('/bin/cpm/package.filterMove' + dir + '.html' + pckgmgr.getCurrentPath(), {
                        index: row.index
                    }, {}, _.bind(function () {
                        this.currentIndex += indexInc;
                        this.refresh();
                    }, this), _.bind(function (result) {
                        core.alert('danger', "Error", "Error on filter move", result);
                    }, this));
                }
            },

            refresh: function (event) {
                if (event) {
                    event.preventDefault();
                    this.getSelectedRow(); // save the current index
                }
                this.table.refresh(this.currentIndex);
                this.getSelectedRow(); // adjust current index
            },

            getSelectedRow: function () {
                var row = undefined;
                var selected = this.table.getSelections();
                if (selected && selected.length > 0) {
                    row = selected[0];
                    this.currentIndex = row.index;
                } else {
                    this.currentIndex = undefined;
                }
                return row;
            },

            resetSelection: function () {
                if (this.currentIndex >= 0) {
                    this.table.setSelection(this.currentIndex);
                }
            }

        });

    })(core.pckgmgr);
    
})(window.core);
