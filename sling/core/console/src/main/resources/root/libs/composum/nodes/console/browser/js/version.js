(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.getAddLabelDialog = function () {
            return core.getView('#version-add-label-dialog', browser.AddLabelDialog);
        };

        browser.getDeleteLabelDialog = function () {
            return core.getView('#version-delete-label-dialog', browser.DeleteLabelDialog);
        };

        browser.getDeleteVersionDialog = function () {
            return core.getView('#version-delete-dialog', browser.DeleteVersionDialog);
        };

        browser.DeleteVersionDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.name = core.getWidget(this.el, 'input[name="name"]', core.components.TextFieldWidget);
                this.$('button.delete').click(_.bind(this.deleteVersion, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="name"]').focus();
                });
                var path = browser.getCurrentPath();
            },

            setVersion: function (version) {
                this.name.setValue(version);
            },

            deleteVersion: function (event) {
                event.preventDefault();
                var path = browser.getCurrentPath();
                var version = this.name.getValue();
                core.ajaxDelete("/bin/cpm/nodes/version.version.json" + path, {
                        data: JSON.stringify({
                            version: version,
                            path: path
                        })
                    },
                    _.bind(function (result) {
                        this.hide();
                    }, this),
                    _.bind(function (result) {
                        this.alert('danger', 'Error on deleting version', result);
                    }, this));

                return false;
            }
        });

        browser.DeleteLabelDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.labelname = core.getWidget(this.el, 'input[name="labelname"]', core.components.TextFieldWidget);
                this.$('button.delete').click(_.bind(this.deleteLabel, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="labelname"]').focus();
                });
                this.labelname.$el.attr('autocomplete', 'off');
                var path = browser.getCurrentPath();
                this.labelname.$el.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        core.getJson('/bin/cpm/nodes/version.labels.json' + path + '?label=' + query,
                            function (data) {
                                callback(data);
                            });
                    }
                });
            },

            deleteLabel: function (event) {
                event.preventDefault();
                var path = browser.getCurrentPath();
                var label = this.labelname.getValue();
                core.ajaxDelete("/bin/cpm/nodes/version.deletelabel.json" + path, {
                    data: JSON.stringify({
                        label: label,
                        path: path
                    })
                }, _.bind(function (result) {
                    this.hide();
                }, this), _.bind(function (result) {
                    this.alert('danger', 'Error on deleting version label', result);
                }, this));

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
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="labelname"]').focus();
                });
            },

            setVersion: function (path, version) {
                this.$path.val(path);
                this.$name.val(version);
            },

            addNewLabel: function (event) {
                event.preventDefault();
                var path = browser.getCurrentPath();
                var version = this.$name.val();
                var label = this.$labelname.getValue();
                core.ajaxPut("/bin/cpm/nodes/version.addlabel.json" + path, JSON.stringify({
                    version: version,
                    label: label,
                    path: path
                }), {
                    //dataType: 'json'
                }, _.bind(function (result) {
                    this.hide();
                }, this), _.bind(function (result) {
                    core.alert('danger', 'Error', 'Error on adding version label', result);
                }, this));

                return false;
            }
        });

        browser.getVersionsTab = function () {
            return core.getView('.node-view-panel .versions', browser.VersionsTab);
        };

        browser.VersionsTab = core.console.DetailTab.extend({
            initialize: function (options) {
                this.table = core.getWidget(this.$el, '.table-container', browser.VersionsTable);
                this.$addButton = this.$('.table-toolbar .add');
                this.$addButton.click(_.bind(this.addLabel, this));
                this.$removeButton = this.$('.table-toolbar .remove');
                this.$removeButton.click(_.bind(this.removeLabel, this));
                this.$deleteButton = this.$('.table-toolbar .delete');
                this.$deleteButton.click(_.bind(this.deleteVersion, this));
                this.$restoreButton = this.$('.table-toolbar .restore');
                this.$restoreButton.click(_.bind(this.restoreVersion, this));
                this.$checkin = this.$('.table-toolbar .checkin');
                this.$checkin.click(_.bind(this.checkin, this));
                this.$checkout = this.$('.table-toolbar .checkout');
                this.$checkout.click(_.bind(this.checkout, this));
                this.$checkpoint = this.$('.table-toolbar .checkpoint');
                this.$checkpoint.click(_.bind(this.checkpoint, this));

                var path = browser.getCurrentPath();
                core.getJson('/bin/cpm/nodes/node.tree.json' + path, _.bind(function (data) {
                    var node = data;
                    if (node.jcrState.isVersionable && node.jcrState.checkedOut) {
                        this.$checkpoint.removeClass('disabled');
                    } else {
                        this.$checkpoint.addClass('disabled');
                    }
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
                }, this));
            },

            reload: function () {
                this.table.loadContent();
            },

            addLabel: function (event) {
                var path = browser.getCurrentPath();
                var dialog = browser.getAddLabelDialog();
                var rows = this.table.getSelections();
                dialog.show(function () {
                    dialog.setVersion(path, rows[0].name);
                }, _.bind(this.reload, this));

            },

            removeLabel: function (event) {
                var dialog = browser.getDeleteLabelDialog();
                dialog.show(undefined, _.bind(this.reload, this));
            },

            checkpoint: function (event) {
                var path = browser.getCurrentPath();
                core.ajaxPost('/bin/cpm/nodes/version.checkpoint.json' + path, {}, {},
                    _.bind(function (result) {
                        core.browser.tree.refresh();
                        core.browser.nodeView.reload();
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error creating checkpoint', result);
                    }, this)
                );
            },

            checkin: function (event) {
                var path = browser.getCurrentPath();
                core.ajaxPost('/bin/cpm/nodes/version.checkin.json' + path, {}, {},
                    _.bind(function (result) {
                        core.browser.tree.refresh();
                        core.browser.nodeView.reload();
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error checking in node', result);
                    }, this)
                );
            },

            checkout: function (event) {
                var path = browser.getCurrentPath();
                core.ajaxPost('/bin/cpm/nodes/version.checkout.json' + path, {}, {},
                    _.bind(function (result) {
                        core.browser.tree.refresh();
                        core.browser.nodeView.reload();
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error checking out node', result);
                    }, this)
                );
            },

            deleteVersion: function (event) {
                var dialog = browser.getDeleteVersionDialog();
                var rows = this.table.getSelections();
                dialog.show(function () {
                    dialog.setVersion(rows.length == 0 ? "" : rows[0].name);
                }, _.bind(this.reload, this));

            },

            restoreVersion: function (event) {
                var rows = this.table.getSelections();
                var version = rows[0].name;
                var path = browser.getCurrentPath();
                core.ajaxPut("/bin/cpm/nodes/version.restore.json" + path, JSON.stringify({
                        version: version,
                        path: path
                    }), {
                        //dataType: 'json'
                    }, _.bind(function (result) {
                        this.reload();
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on restoring version label', result);
                    }, this)
                );
            }
        });

        browser.VersionsTable = Backbone.View.extend({
            initialize: function (options) {
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
                    rowStyle: _.bind(function (row, index) {
                        return {
                            classes: (row.current ? 'editable current' : 'protected')
                        };
                    }, this),

                    columns: [{
                        class: 'selection',
                        radio: true,
                        sortable: false
                    }, {
                        class: 'name',
                        field: 'name',
                        title: 'Name'
                    }, {
                        class: 'date',
                        field: 'date',
                        title: 'Date'
                    }, {
                        class: 'labels',
                        field: 'labels',
                        title: 'Labels',
                        formatter: _.bind(this.formatValue, this)
                    }]
                });

            },

            formatValue: function (value, row, index) {
                var labels = "&nbsp;";
                for (var i in value) {
                    labels = labels + '<span class="label label-primary">' + value[i] + '</span>\n';
                }
                return labels;
            },

            getSelections: function () {
                var rows = this.$table.bootstrapTable('getSelections');
                return rows;
            },

            loadContent: function () {
                var path = browser.getCurrentPath();
                this.state.load = true;
                core.getJson("/bin/cpm/nodes/version.versions.json" + path,
                    _.bind(function (result) {
                        this.$table.bootstrapTable('load', result);
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on loading properties', result);
                    }, this), _.bind(function (result) {
                        this.state.load = false;
                    }, this)
                );
            }

        })

    })(core.browser);

})(window.core);
