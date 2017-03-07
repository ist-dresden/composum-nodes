/**
 *
 *
 */
(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components) {

        /**
         * the basic dialog component as 'superclass' for all dialog components
         */
        components.Dialog = Backbone.View.extend({

            initialize: function (options) {
                this.$alert = this.$('.alert');
                this.setUpWidgets(this.el);
                this.$el.on('shown.bs.modal', _.bind(this.onShown, this));
            },

            setUpWidgets: function(root) {
                window.widgets.setUp(root);
            },

            /**
             * returns the widget instance (view) for a DOM element
             */
            widgetOf: function (element) {
                return core.widgetOf(element);
            },

            show: function (initView, callback) {
                this.initView = initView;
                this.callback = callback;
                this.$el.modal('show');
            },

            onShown: function () {
                this.resetOnShown();
                if (_.isFunction(this.initView)) {
                    this.initView();
                }
            },

            resetOnShown: function() {
                this.reset();
            },

            hide: function () {
                this.$el.modal('hide');
                if (_.isFunction(this.callback)) {
                    this.callback();
                }
                this.reset();
            },

            /**
             * Resets all form values to an initial or undefined value.
             */
            reset: function () {
                this.alert();
                // reset all default HTML component elements
                this.$('input,textarea,select').each(function () {
                    if (this.type !== 'radio' && this.type !== 'hidden') {
                        $(this).val(undefined);
                    }
                });
                // reset all known View instances using their 'reset' method
                this.$('.widget').each(function () {
                    if (this.view) {
                        if (_.isFunction(this.view.reset)) {
                            this.view.reset.apply(this.view);
                        }
                    }
                });
            },

            /**
             * Displays a message text in a dialogs predefined alert box.
             * @param type the message error level (success, info, warning, danger)
             * @param message the message text to display; optional - if not present the alert will hide
             * @param result an optional result object from an Ajax call; a hint from this result is added to the text
             */
            alert: function (type, message, result) {
                this.$alert.removeClass();
                if (message) {
                    this.$alert.html(result ? core.resultMessage(result, message) : message);
                    this.$alert.addClass('alert').addClass('alert-' + type);
                } else {
                    this.$alert.html('');
                    this.$alert.addClass('alert').addClass('alert-hidden');
                }
            },

            /**
             * Submit the form of the dialog.
             * @param onSuccess an optional callback function called after a successful request
             */
            submitForm: function (onSuccess, onError, onComplete) {
                core.submitForm(this.el,
                    _.bind(function (result) {
                        if (_.isFunction(onSuccess)) {
                            onSuccess(result);
                        }
                        this.hide();
                    }, this),
                    _.bind(function (result) {
                        if (_.isFunction(onError)) {
                            onError(result);
                        } else {
                            if (onError === undefined || onError) {
                                this.alert('danger', "Error on submit", result);
                            }
                        }
                    }, this),
                    onComplete
                );
            },

            submitFormPut: function (onSuccess, onError, onComplete) {
                var form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                core.submitFormPut(this.el,
                    form ? form.getValues.apply(form) : undefined,
                    _.bind(function (result) {
                        if (_.isFunction(onSuccess)) {
                            onSuccess(result);
                        }
                        this.hide();
                    }, this),
                    _.bind(function (result) {
                        if (_.isFunction(onError)) {
                            onError(result);
                        } else {
                            if (onError === undefined || onError) {
                                this.alert('danger', "Error on submit", result);
                            }
                        }
                    }, this),
                    onComplete
                );
            },

            submitPUT: function (label, url, data, onSuccess) {
                core.ajaxPut(url, JSON.stringify(data), {
                    dataType: 'json'
                }, onSuccess, undefined, _.bind(function (result) {
                    if (result.status == 200) {
                        if (_.isFunction(onSuccess)) {
                            onSuccess(result);
                        }
                        this.hide();
                    } else {
                        this.alert('danger', 'Error on ' + label, result);
                    }
                }, this));
            }
        });

        /**
         * the dialog to select a repository path in a tree view
         */
        components.SelectPathDialog = components.Dialog.extend({

            initialize: function (options) {
                components.Dialog.prototype.initialize.apply(this, [options]);
                this.busy = false;
                this.tree = core.getView(this.$('.path-select-tree'), components.Tree);
                this.tree.onNodeSelected = _.bind(this.onNodeSelected, this);
                this.$title = this.$('.modal-title');
                this.$label = this.$('.path-input-label');
                this.$input = this.$('input.path-input');
                this.$input.on('change', _.bind(this.inputChanged, this));
                this.$('button.select').click(_.bind(function () {
                    if (_.isFunction(this.callback)) {
                        this.callback(this.getValue());
                    }
                    this.hide();
                }, this));
            },

            /**
             * initialization after shown...
             */
            onShown: function () {
                core.components.Dialog.prototype.onShown.apply(this);
                this.inputChanged(); // simulate a change after shown to initialize the tree
            },

            /**
             * defines the dialog title (default: 'default' data attribute of the title element)
             */
            setTitle: function (title) {
                this.$title.text(title ? title : this.$title.data('default'));
            },

            /**
             * defines the input field label (default: 'default' data attribute of the label element)
             */
            setLabel: function (label) {
                this.$label.text(label ? label : this.$label.data('default'));
            },

            /**
             * defines the root path for the tree (default: '/')
             */
            setRootPath: function (rootPath) {
                this.tree.setRootPath(rootPath);
            },

            /**
             * defines the node filter for the tree; should be set according to the current value type
             */
            setFilter: function (filter) {
                this.tree.setFilter(filter);
            },

            /**
             * returns the current path value selected in this dialog
             */
            getValue: function () {
                return this.$input.val();
            },

            /**
             * defines the (initial) value - the curent / old value
             */
            setValue: function (value) {
                this.$input.val(value);
                this.inputChanged(); // select vaöue in the tree
            },

            /**
             * the callback on each change in the input field;
             * selects the node in the tree view if the nodes exists
             */
            inputChanged: function () {
                if (!this.busy) {
                    this.busy = true;
                    var path = this.getValue();
                    if (path.indexOf('/') === 0) {
                        core.getJson('/bin/cpm/nodes/node.tree.json' + path, _.bind(function (data) {
                            this.tree.selectNode.apply(this.tree, [data.path]);
                        }, this));
                    }
                    this.busy = false;
                }
            },

            /**
             * callback from the tree if a node is selected;
             * sets the select path in the input field
             */
            onNodeSelected: function (path) {
                if (!this.busy) {
                    this.busy = true;
                    this.setValue(path);
                    this.busy = false;
                }
            },

            /**
             * extended to reset the selection in the tree
             */
            reset: function () {
                components.Dialog.prototype.reset.apply(this);
                this.tree.reset.apply(this.tree);
            }
        });

    })(core.components);

})(window.core);
