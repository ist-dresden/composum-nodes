/**
 *
 *
 */
(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components) {

        components.const = components.const || {};
        components.const.dialog = {
            alert: {
                type: {
                    error: 'danger',
                    warn: 'warning'
                }
            },
            translate: {
                uri: {
                    object: '/bin/cpm/core/translate.object.json',
                    status: '/bin/cpm/core/translate.status.json'
                }
            },
            load: {
                base: '/libs/composum/nodes/commons/components/dialogs',
                _path: '.path-select.html'
            }
        };

        /**
         * the basic dialog component as 'superclass' for all dialog components
         */
        components.Dialog = Backbone.View.extend({

            initialize: function (options) {
                this.isLoaded = options.loaded || false;
                this.$alert = this.$('.alert');
                this.$messageHead = this.$('.messages .panel-heading');
                this.$messageBody = this.$('.messages .panel-body');
                this.setUpWidgets(this.el);
                this.$el.on('shown.bs.modal', _.bind(this.onShown, this));
                if (this.isLoaded) { // remove a loaded dialog on close
                    this.$el.on('hidden.bs.modal', _.bind(this.destroy, this));
                }
            },

            setUpWidgets: function (root) {
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
                    this.initView(this);
                }
            },

            resetOnShown: function () {
                if (!this.isLoaded) { // asuming that loaded dialogs are pre filled
                    this.reset();
                }
            },

            hide: function () {
                this.$el.modal('hide');
                if (_.isFunction(this.callback)) {
                    this.callback(this);
                }
                this.reset();
            },

            /**
             * Resets all form values to an initial or undefined value.
             */
            reset: function () {
                this.alert();
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
             * remove dialog from the DOM (on closing loaded dialogs)
             */
            destroy: function (event) {
                this.$el.remove();
            },

            /**
             * Displays a message text in a dialogs predefined alert or message panel box.
             * @param type the message error level (success, info, warning, danger)
             * @param message the message text to display; optional - if not present the alert will hide
             * @param result an optional result object from an Ajax call; a hint from this result is added to the text
             */
            alert: function (type, message, result) {
                if (message) {
                    type = components.const.dialog.alert.type[type] || type;
                    if (this.$messageBody.length === 1) {
                        this.$messageHead.html(result ? core.resultMessage(result, message) : message);
                        this.$messageHead.removeClass('hidden');
                        this.$messageBody.html('');
                        this.$messageBody.addClass('hidden');
                        this.$messageBody.parent().removeClass().addClass('panel').addClass('panel-' + type);
                    } else {
                        this.$alert.html(result ? core.resultMessage(result, message) : message);
                        this.$alert.removeClass().addClass('alert').addClass('alert-' + type);
                    }
                } else {
                    if (this.$messageBody.length === 1) {
                        this.$messageBody.parent().removeClass().addClass('hidden');
                        this.$messageHead.html('');
                        this.$messageBody.html('');
                    } else {
                        this.$alert.html('');
                        this.$alert.removeClass().addClass('alert').addClass('hidden');
                    }
                }
            },

            /**
             * Displays a message list with title in a dialogs predefined message panel box.
             * @param type the message error level (success, info, warning, danger)
             * @param title the message text to display in the heading; optional - if not present the box will hide
             * @param messages the message list with items like {level:(error,warn,info),text:'...'}
             */
            messages: function (type, title, messages) {
                if (this.$messageBody.length === 1) {
                    type = components.const.dialog.alert.type[type] || type;
                    this.$messageBody.parent().removeClass();
                    if (title) {
                        this.$messageHead.html(title);
                        this.$messageHead.removeClass('hidden');
                        this.$messageBody.parent().addClass('panel').addClass('panel-' + type);
                    } else {
                        this.$messageHead.addClass('hidden');
                    }
                    this.$messageBody.html('');
                    if (_.isArray(messages) && messages.length > 0) {
                        this.$messageBody.html('<ul></ul>');
                        var $list = this.$messageBody.find('ul');
                        for (var i = 0; i < messages.length; i++) {
                            var level = messages[i].level;
                            level = components.const.dialog.alert.type[level] || level;
                            $list.append('<li class="bg-' + level + ' text-' + level + '">'
                                + (messages[i].label ? '<span class="label">' + messages[i].label + ':</span>' : '')
                                + messages[i].text
                                + (messages[i].hint ? '<span class="hint">(' + messages[i].hint + ')</span>' : '')
                                + '</li>')
                        }
                        this.$messageBody.removeClass('hidden');
                    } else {
                        this.$messageBody.html('');
                        this.$messageBody.addClass('hidden');
                    }
                } else {
                    this.alert(type, title);
                }
            },

            /**
             * Submit the form of the dialog.
             * @param onSuccess optional; 'true' or a callback function called after a successful request
             */
            submitForm: function (onSuccess, onError, onComplete) {
                core.submitForm(this.el,
                    _.bind(function (result) {
                        if (_.isFunction(onSuccess)) {
                            onSuccess(result);
                        } else {
                            if (onSuccess) { // use 'true' to show the success messages
                                if (_.isObject(result) && _.isObject(result.response)) {
                                    var response = result.response;
                                    var messages = result.messages;
                                    core.messages(response.level, response.text, messages);
                                }
                            }
                        }
                        this.hide();
                    }, this),
                    _.bind(function (xhr) {
                        if (_.isFunction(onError)) {
                            onError(xhr);
                        } else {
                            if (onError === undefined || onError /* maybe 'false' */) {
                                this.onError(xhr);
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
                        } else {
                            if (onSuccess) { // use 'true' to show the success messages
                                if (_.isObject(result) && _.isObject(result.response)) {
                                    var response = result.response;
                                    var messages = result.messages;
                                    core.messages(response.level, response.text, messages);
                                }
                            }
                        }
                        this.hide();
                    }, this),
                    _.bind(function (xhr) {
                        if (_.isFunction(onError)) {
                            onError(xhr);
                        } else {
                            if (onError === undefined || onError /* maybe 'false' */) {
                                this.onError(xhr);
                            }
                        }
                    }, this),
                    onComplete
                );
            },

            submitPUT: function (label, url, data, onSuccess) {
                core.ajaxPut(url, JSON.stringify(data), {
                        dataType: 'json'
                    },
                    onSuccess, _.bind(this.onError, this), _.bind(function (xhr) {
                        if (xhr.status >= 200 && xhr.status < 299) {
                            if (xhr.status === 200 && _.isFunction(onSuccess)) {
                                onSuccess(xhr);
                                this.hide();
                            } else {
                                var detail = xhr.responseJSON;
                                if (xhr.status !== 200 && _.isObject(detail) && detail.messages) {
                                    this.onError(xhr, label);
                                } else {
                                    this.hide();
                                }
                            }
                        } else {
                            this.onError(xhr, label);
                        }
                    }, this));
            },

            onError: function (xhr, title) {
                if (xhr.responseJSON && xhr.responseJSON.messages) {
                    var status = xhr.responseJSON;
                    this.messages(status.success ? (status.warning ? 'warn' : 'info') : 'error',
                        status.title || title, status.messages);
                } else {
                    this.errorMessage(title ? title : "Error", xhr);
                }
            },

            errorMessage: function (message, result) {
                var detail = result.responseJSON;
                if (_.isObject(detail) && detail.response) {
                    this.messages(detail.response.level, detail.response.text, detail.messages);
                } else {
                    var level = result.status >= 300 && result.status < 399 ? 'info' : 'danger';
                    this.alert(level, message, result);
                }
            }
        });

        /**
         * a dialog which can be 'opened' inside of another open dialog
         */
        components.StackableDialog = components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.call(this, options);
            },

            /**
             *
             */
            show: function (initView, callback) {
                core.components.Dialog.prototype.show.call(this, initView, callback);
            },

            hide: function () {
                core.components.Dialog.prototype.hide.call(this);
            }
        });

        components.AbstractPathSelectDialog = components.StackableDialog.extend({

            initialize: function (options) {
                components.StackableDialog.prototype.initialize.call(this, options);
                this.$title = this.$('.modal-title');
                this.$label = this.$('.path-input-label');
                this.input = core.getView(this.$(options.inputSelector || 'input.path-input'),
                    options.inputType || core.components.PathWidget);
                this.input.$el.on('change', _.bind(this.inputChanged, this));
            },

            /**
             * @extends components.StackableDialog - initialization after shown...
             */
            onShown: function () {
                components.StackableDialog.prototype.onShown.call(this);
                this.inputChanged(); // simulate a change after shown to initialize the view
            },

            /**
             * @override prevent from default callback handling;
             * the callback must be triggerded by 'select' with the value (path) as parameter
             */
            hide: function () {
                this.$el.modal('hide');
                this.reset();
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
             * defines the root path for the selector (default: '/')
             */
            setRootPath: function (rootPath) {
                this.input.setRootPath(rootPath);
            },

            /**
             * defines the node filter for the view; should be set according to the current value type
             */
            setFilter: function (filter) {
                this.input.setFilter(filter);
            },

            /**
             * returns the current path value selected in this dialog
             */
            getValue: function () {
                return this.input.getValue();
            },

            /**
             * defines the (initial) value - the current / old value
             */
            setValue: function (value) {
                this.input.setValue(value);
                this.inputChanged(); // trigger state refresh
            },

            /**
             * @abstract the callback on each change in the input field
             */
            inputChanged: function () {
            }
        });

        /**
         * the dialog to select a repository path in a tree view
         */
        components.SelectPathDialog = components.AbstractPathSelectDialog.extend({

            initialize: function (options) {
                components.AbstractPathSelectDialog.prototype.initialize.apply(this, [options]);
                this.busy = false;
                this.tree = core.getView(this.$('.path-select-tree'), components.Tree);
                this.tree.onNodeSelected = _.bind(this.onNodeSelected, this);
                this.$('button.select').click(_.bind(function () {
                    if (_.isFunction(this.callback)) {
                        this.callback(this.getValue());
                    }
                    this.hide();
                }, this));
            },

            /**
             * defines the root path for the tree (default: '/')
             */
            setRootPath: function (rootPath) {
                components.AbstractPathSelectDialog.prototype.setRootPath.call(this, rootPath);
                this.tree.setRootPath(rootPath);
            },

            /**
             * defines the node filter for the tree; should be set according to the current value type
             */
            setFilter: function (filter) {
                components.AbstractPathSelectDialog.prototype.setFilter.call(this, filter);
                this.tree.setFilter(filter);
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
                        core.getJson('/bin/cpm/nodes/node.tree.json' + core.encodePath(path), _.bind(function (data) {
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
                components.AbstractPathSelectDialog.prototype.reset.apply(this);
                this.tree.reset.apply(this.tree);
            }
        });

        /**
         * load dialogs on demand and remove them on close
         */
        components.LoadedDialog = components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.call(this, _.extend({
                    loaded: true
                }, options));
            }
        });

        core.addLoadedDialog = function (viewType, html, config) {
            var $body = $('body');
            $body.append(html);
            var $dialog = $body.children(':last-child');
            return core.getWidget($body, $dialog[0], viewType, config
                ? _.extend({loaded: true}, config) : {loaded: true});
        };

        core.showLoadedDialog = function (viewType, html, config, initView, callback) {
            var dialog = core.addLoadedDialog(viewType, html, config);
            if (dialog) {
                dialog.show(initView, callback);
            }
        };

        core.openLoadedDialog = function (url, viewType, config, initView, callback) {
            core.getHtml(url, function (content) {
                core.showLoadedDialog(viewType, content, config, initView, callback);
            });
        };

        /**
         * a FormDialog supports validation before submit
         * uses a 'config' attribute:
         * config {
         *     validationUrl: a function or value for validation roundtrip and i18n translation of validation messages
         * }
         */
        components.FormDialog = components.LoadedDialog.extend({

            initialize: function (options) {
                var formType = options ? (options.formType || core.components.FormWidget) : core.components.FormWidget;
                this.form = core.getWidget(this.el, "form", formType);
                core.components.LoadedDialog.prototype.initialize.call(this, options);
                this.validationHints = [];
                this.initView();
                this.initSubmit();
            },

            getConfig: function () {
                return this.config;
            },

            dialogData: function () {
                return {};
            },

            initSubmit: function () {
                this.$('form').on('submit', _.bind(this.onSubmit, this));
            },

            initView: function () {
            },

            validationReset: function () {
                this.$alert.addClass('alert-hidden');
                this.$alert.html('');
                this.validationHints = [];
                this.form.validationReset();
            },

            onValidationFault: function () {
                this.form.onValidationFault();
            },

            message: function (type, label, message, hint) {
                if (message) {
                    this.alert(type, '<div class="text-danger"><span class="label">' + label
                        + '</span><span class="message">'
                        + message + (hint ? " (" + hint + ")" : '') + '</span></div>');
                }
            },

            hintsMessage: function (level) {
                this.messages(level ? level : 'warning', this.validationHints.length < 1
                    ? 'validation error' : undefined, this.validationHints);
            },

            validationHint: function (type, label, message, hint) {
                if (message) {
                    this.validationHints.push({level: type, label: label, text: message, hint: hint});
                }
            },

            validateForm: function () {
                this.validationReset();
                return this.form.validate(_.bind(this.validationHint, this));
            },

            /**
             * @returns {{dialog: (*|{}), messages: Array}} the data for a validation roundtrip
             */
            validationData: function () {
                return {
                    dialog: this.dialogData(),
                    messages: this.validationHints
                };
            },

            /**
             * the validation strategy with support for an asynchronous validation call;
             * if a validationUrl configuration is supported a PUT request with the current validation hints is sent
             * via PUT to that url for extended validation and/or validation hints i18n translation
             * @param onSuccess called after successful validation
             * @param onError called if a validation fault registered
             */
            doValidate: function (onSuccess, onError) {
                var valid = this.validateForm();
                var config = this.getConfig();
                if (config && config.validationUrl) {
                    var url = _.isFunction(config.validationUrl) ? config.validationUrl() : config.validationUrl;
                    var data = this.validationData();
                    core.ajaxPut(url, JSON.stringify(data), {
                            dataType: 'json'
                        }, undefined, undefined,
                        _.bind(function (xhr) {
                            var result = xhr.responseJSON;
                            if (result) {
                                if (result.messages) {
                                    this.validationHints = result.messages;
                                }
                                if (result.success) {
                                    onSuccess();
                                } else {
                                    onError();
                                }
                            } else {
                                this.onError(xhr);
                            }
                        }, this))
                } else {
                    if (valid) {
                        onSuccess();
                    } else {
                        onError();
                    }
                }
            },

            /**
             * @default the simplest 'save and return' - no message - override if not enough
             */
            doSubmit: function () {
                this.submitForm();
            },

            /**
             * triggered if the submit button is clicked or activated somewhere else
             */
            onSubmit: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.form.prepare();
                this.doValidate(_.bind(function () {
                    this.form.finalize();
                    this.doSubmit(_.bind(function (result) {
                        this.showResult(result);
                    }, this));
                }, this), _.bind(function () {
                    if (this.validationHints.length < 1) {
                        core.i18n.get('Validation error', _.bind(function (text) {
                            this.messages('warning', text, this.validationHints);
                        }, this));
                    } else {
                        this.messages('warning', undefined, this.validationHints);
                    }
                    this.onValidationFault();
                }, this));
                return false;
            },

            showResult: function (result) {
                if (_.isObject(result) && _.isObject(result.response)) {
                    var response = result.response;
                    var messages = result.messages;
                    if (_.isArray(messages) && messages.length > 0) {
                        core.messages(response.level, response.text, messages);
                    } else {
                        core.alert(response.level, response.title, response.text);
                    }
                }
            }
        });

        core.showFormDialog = function (viewType, html, config, initView, callback) {
            core.showLoadedDialog(viewType, html, config, initView, callback);
        };

        core.openFormDialog = function (url, viewType, config, initView, callback) {
            core.getHtml(url, function (content) {
                core.showLoadedDialog(viewType, content, config, initView, callback);
            });
        };

    })(core.components);

})(window.core);
