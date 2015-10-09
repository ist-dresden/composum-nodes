(function(core) {
    'use strict';

    core.browser = core.browser || {};

    (function(browser) {

        browser.getAddLabelDialog = function() {
            return core.getView('#version-add-label-dialog', browser.AddLabelDialog);
        };

        browser.getDeleteLabelDialog = function() {
            return core.getView('#version-delete-label-dialog', browser.DeleteLabelDialog);
        };

        browser.getDeleteVersionDialog = function() {
            return core.getView('#version-delete-dialog', browser.DeleteVersionDialog);
        };

        browser.DeleteVersionDialog = core.components.Dialog.extend({
            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.name = core.getWidget(this.el, 'input[name="name"]', core.components.TextFieldWidget);
                this.$('button.delete').click(_.bind(this.deleteVersion, this));
                this.$el.on('shown.bs.modal', function() {
                    $(this).find('input[name="name"]').focus();
                });
                var path = browser.getCurrentPath();
            },

            reset: function() {
            },

            setVersion: function (version) {
                this.name.setValue(version);
            },

            deleteVersion: function(event) {
                event.preventDefault();
                var path = browser.getCurrentPath();
                var version = this.name.getValue();
                $.ajax({
                    url: "/bin/core/version.version.json" + path,
                    data: JSON.stringify({
                        version: version,
                        path: path
                    }),
                    //dataType: 'json',
                    type: 'DELETE',
                    success: _.bind (function (result) {
                        this.hide();
                    }, this),
                    error: _.bind (function (result) {
                        core.alert('danger', 'Error', 'Error on deleting version', result);
                    }, this)
                });

                return false;
            }
        });

        browser.DeleteLabelDialog = core.components.Dialog.extend({
            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.labelname = core.getWidget(this.el, 'input[name="labelname"]', core.components.TextFieldWidget);
                this.$('button.delete').click(_.bind(this.deleteLabel, this));
                this.$el.on('shown.bs.modal', function() {
                    $(this).find('input[name="labelname"]').focus();
                });
                this.labelname.$el.attr('autocomplete', 'off');
                var path = browser.getCurrentPath();
                this.labelname.$el.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        $.get('/bin/core/version.labels.json'+ path + '?label=' + query, {
                        }, function (data) {
                            callback(data);
                        });
                    }
                });

            },

            reset: function() {
                this.labelname.setValue("");
            },

            deleteLabel: function(event) {
                event.preventDefault();
                var path = browser.getCurrentPath();
                var label = this.labelname.getValue();
                $.ajax({
                    url: "/bin/core/version.deletelabel.json" + path,
                    data: JSON.stringify({
                        label: label,
                        path: path
                    }),
                    //dataType: 'json',
                    type: 'DELETE',
                    success: _.bind (function (result) {
                        this.hide();
                    }, this),
                    error: _.bind (function (result) {
                        core.alert('danger', 'Error', 'Error on deleting version label', result);
                    }, this)
                });

                return false;
            }
        });

        browser.AddLabelDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$path = this.$('input[name="path"]');
                this.$name = this.$('input[name="name"]');
                this.$labelname = core.getWidget(this.el, 'input[name="labelname"]', core.components.TextFieldWidget);
                this.$('button.create').click(_.bind(this.addNewLabel, this));
                this.$el.on('shown.bs.modal', function() {
                    $(this).find('input[name="labelname"]').focus();
                });
            },

            reset: function() {
//                core.components.Dialog.prototype.reset.apply(this);
                this.$labelname.setValue("");
            },

            setVersion: function (path, version) {
                this.$path.val(path);
                this.$name.val(version);
            },

            addNewLabel: function(event) {
                event.preventDefault();
                var path = browser.getCurrentPath();
                var version = this.$name.val();
                var label = this.$labelname.getValue();
                $.ajax({
                    url: "/bin/core/version.addlabel.json" + path,
                    data: JSON.stringify({
                        version: version,
                        label: label,
                        path: path
                    }),
                    //dataType: 'json',
                    type: 'PUT',
                    success: _.bind (function (result) {
                        this.hide();
                    }, this),
                    error: _.bind (function (result) {
                        core.alert('danger', 'Error', 'Error on adding version label', result);
                    }, this)
                });

                return false;
            }
        });

        browser.getVersionsTab = function() {
            return core.getView('.node-view-panel .versions', browser.VersionsTab);
        };

        browser.VersionsTab = browser.NodeTab.extend({
            initialize: function(options) {
                this.table = core.getWidget(this.$el, '.table-container', browser.VersionsTable);
                this.$addButton = this.$('.table-toolbar .add');
                this.$addButton.click(_.bind (this.addLabel, this));
                this.$removeButton = this.$('.table-toolbar .remove');
                this.$removeButton.click(_.bind (this.removeLabel, this));
                this.$deleteButton = this.$('.table-toolbar .delete');
                this.$deleteButton.click(_.bind (this.deleteVersion, this));
                this.$restoreButton = this.$('.table-toolbar .restore');
                this.$restoreButton.click(_.bind (this.restoreVersion, this));
                this.$checkin = this.$('.table-toolbar .checkin');
                this.$checkin.click(_.bind (this.checkin, this));
                this.$checkout = this.$('.table-toolbar .checkout');
                this.$checkout.click(_.bind (this.checkout, this));
                var node = browser.tree.current();
                if (node.jcrState.isVersionable) {
                    this.$checkin.removeClass('disabled');
                    this.$checkout.removeClass('disabled');
                    this.$restoreButton.removeClass('disabled');
                    this.$deleteButton.removeClass('disabled');
                    this.$removeButton.removeClass('disabled');
                    this.$addButton.removeClass('disabled');
                } else {
                    this.$checkin.addClass('disabled');
                    this.$checkout.addClass('disabled');
                    this.$restoreButton.addClass('disabled');
                    this.$deleteButton.addClass('disabled');
                    this.$removeButton.addClass('disabled');
                    this.$addButton.addClass('disabled');
                }
            },

            reload: function() {
                this.table.loadContent();
            },

            addLabel: function(event) {
                var path = browser.getCurrentPath();
                var dialog = browser.getAddLabelDialog();
                var rows = this.table.getSelections();
                dialog.show(undefined, _.bind (this.reload, this));
                dialog.setVersion(path, rows[0].name);
            },

            removeLabel: function(event) {
                var dialog = browser.getDeleteLabelDialog();
                dialog.show(undefined, _.bind (this.reload, this));
            },

            checkin: function(event) {
                var path = browser.getCurrentPath();
                $.ajax({
                    method: 'POST',
                    url: '/bin/core/version.checkin.json' + path,
                    success: _.bind (function(result) {
                        core.browser.tree.refresh();
                        core.browser.nodeView.reload();
                    }, this),
                    error: _.bind (function(result) {
                        core.alert('danger', 'Error', 'Error checking in node', result);
                    }, this)
                });
            },

            checkout: function(event) {
                var path = browser.getCurrentPath();
                $.ajax({
                    method: 'POST',
                    url: '/bin/core/version.checkout.json' + path,
                    success: _.bind (function(result) {
                        core.browser.tree.refresh();
                        core.browser.nodeView.reload();
                    }, this),
                    error: _.bind (function(result) {
                        core.alert('danger', 'Error', 'Error checking out node', result);
                    }, this)
                });
            },

            deleteVersion: function(event) {
                var dialog = browser.getDeleteVersionDialog();
                var rows = this.table.getSelections();
                dialog.show(undefined, _.bind (this.reload, this));
                dialog.setVersion(rows.length == 0 ? "" : rows[0].name);
            },

            restoreVersion: function(event) {
                var rows = this.table.getSelections();
                var version = rows[0].name;
                var path = browser.getCurrentPath();
                $.ajax({
                    url: "/bin/core/version.restore.json" + path,
                    data: JSON.stringify({
                        version: version,
                        path: path
                    }),
                    //dataType: 'json',
                    type: 'PUT',
                    success: _.bind (function (result) {
                        this.reload();
                    }, this),
                    error: _.bind (function (result) {
                        core.alert('danger', 'Error', 'Error on restoring version label', result);
                    }, this)
                });

            }
        });

        browser.VersionsTable = Backbone.View.extend({
            initialize: function(options) {
                this.state = {
                    load: false
                };

                this.$table = this.$('.version-table');
                this.$table.bootstrapTable({

                    search: true,
                    showToggle: false,
                    striped: true,
                    singleSelect: true,
                    clickToSelect: true,
                    rowStyle: _.bind (function(row,index) {
                        return {
                            classes: (row.current ? 'editable current' : 'protected')
                        };
                    }, this),

                    columns: [{
                        class: 'selection',
                        radio: true,
                        sortable: false
                    },{
                        class: 'name',
                        field: 'name',
                        title: 'Name'
                    },
                    {
                        class: 'date',
                        field: 'date',
                        title: 'Date'
                    },
                    {
                        class: 'labels',
                        field: 'labels',
                        title: 'Labels',
                        formatter: _.bind (this.formatValue, this)
                    }]
                });

            },

            formatValue: function(value,row,index) {
                var labels = "&nbsp;";
                for (var i in value) {
                    labels = labels + '<span class="label label-primary">' + value[i] + '</span>\n';
                }
                return labels;
            },

            getSelections: function() {
                var rows = this.$table.bootstrapTable('getSelections');
                return rows;
            },

            loadContent: function() {
                var path = browser.getCurrentPath();
                this.state.load = true;
                $.ajax({
                    url: "/bin/core/version.versions.json" + path,
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

        })

    })(core.browser);

})(window.core);
