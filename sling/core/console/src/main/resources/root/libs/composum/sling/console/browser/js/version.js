(function(core) {
    'use strict';

    core.browser = core.browser || {};

    (function(browser) {

        browser.getAddLabelDialog = function() {
            return core.getView('#version-add-label-dialog', browser.AddLabelDialog);
        };

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
            },

            reload: function() {
                this.table.loadContent();
            },

            addLabel: function(event) {
                var path = browser.getCurrentPath();
                var dialog = browser.getAddLabelDialog();
                var rows = this.table.getSelections();
                dialog.show(undefined, _.bind (this.reload, this));
                dialog.setVersion(path, rows[0].name); //TBD
            },

            removeLabel: function(event) {
                var path = browser.getCurrentPath();
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
                        title: 'Labels'
                    }]
                });

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
