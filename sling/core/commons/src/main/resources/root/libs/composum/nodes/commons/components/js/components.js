/**
 *
 *
 */
(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components, widgets) {

        //
        // Form
        //

        components.const = _.extend(components.const || {}, {
            form: {
                css: {
                    base: 'form-widget',
                    action: {
                        slingPost: 'form-action_Sling-POST'
                    },
                    status: {
                        valid: 'valid-form',
                        invalid: 'invalid-form'
                    },
                    selector: {
                        item: '.multi-form-item'
                    }
                }
            }
        });

        /**
         * the 'widget-form'
         *
         * the widget to handle a HTML form element and its embedded widgets.
         * - validates all widgets in the form (found by their 'widget' class)
         * - toggles the classes 'valid-form / 'invalid-form'
         *
         * This 'Widget' doesn't cache or store the validation result!
         */
        components.FormWidget = Backbone.View.extend({

            initialize: function (options) {
                var c = components.const.form;
                this.isSlingPost = this.$el.hasClass(c.css.action.slingPost);
            },

            /**
             * the widgets 'isValid' always performs a 'validation' of the form
             */
            isValid: function (alertMethod) {
                return this.validate(alertMethod);
            },

            validationReset: function () {
                this.$(widgets.const.css.selector.general).each(function () {
                    if (this.view && _.isFunction(this.view.validationReset)) {
                        this.view.validationReset.apply(this.view);
                    }
                });
            },

            /**
             * the validation calls the 'isValid' function of each widget in the form;
             * the class of the form signals the result ('valid-form / 'invalid-form')
             */
            validate: function (alertMethod) {
                var c = components.const.form;
                var valid = true;
                this.$(widgets.const.css.selector.general).each(function () {
                    if (this.view) {
                        if (_.isFunction(this.view.isValid)) {
                            // check each widget independent from the current result
                            valid = (this.view.isValid.apply(this.view, [alertMethod]) && valid);
                        }
                    }
                });
                if (valid) {
                    this.$el.removeClass(c.css.status.invalid);
                    this.$el.addClass(c.css.status.valid);
                } else {
                    this.$el.removeClass(c.css.status.valid);
                    this.$el.addClass(c.css.status.invalid);
                }
                return valid;
            },

            /**
             * returns the current set of values of the entire form
             */
            getValues: function () {
                var c = components.const.form;
                var values = {};
                this.$('.widget').each(function () {
                    if (this.view) {
                        if (this.view.$el.parent().closest(c.css.selector.item).length == 0 &&
                            _.isFunction(this.view.getValue)) {
                            var name = core.getWidgetNames(this.view);
                            // store 'structured names' in a complex object...
                            var object = values;
                            for (var i = 0; i < name.length; i++) {
                                if (i < name.length - 1) {
                                    object[name[i]] = object[name[i]] || {};
                                    object = object[name[i]];
                                } else {
                                    object[name[i]] = this.view.getValue.apply(this.view);
                                }
                            }
                        }
                    }
                });
                return values;
            },

            /**
             * presets the values of the entire form
             */
            setValues: function (values) {
                var c = components.const.form;
                this.$('.widget').each(function () {
                    if (this.view) {
                        if (this.view.$el.parent().closest(c.css.selector.item).length == 0 &&
                            _.isFunction(this.view.setValue)) {
                            var name = core.getWidgetNames(this.view);
                            // map complex object to 'structured names'...
                            var object = values;
                            for (var i = 0; i < name.length; i++) {
                                if (i < name.length - 1) {
                                    if (object) {
                                        object = object[name[i]];
                                    }
                                } else {
                                    this.view.setValue.apply(this.view, [object ? object[name[i]] : undefined]);
                                }
                            }
                        }
                    }
                });
            },

            /**
             * a reset of a form resets all widgets calling their 'reset' function
             */
            reset: function () {
                this.$('.widget').each(function () {
                    if (this.view) {
                        if (_.isFunction(this.view.reset)) {
                            this.view.reset.apply(this.view);
                        }
                    }
                });
            },

            /**
             * prepare the validation and a following submit (adjust names and values is useful)
             */
            prepare: function () {
                var c = components.const.form;
                this.$(widgets.const.css.selector.general).each(function () {
                    if (this.view) {
                        if (_.isFunction(this.view.prepare)) {
                            // prepare each widget independent
                            this.view.prepare.apply(this.view);
                        }
                    }
                });
            },

            /**
             * Submit the form of the dialog.
             */
            submitForm: function (onSuccess, onError, onComplete) {
                core.submitForm(this.el, onSuccess, onError, onComplete);
            },

            /**
             * Submit the form data using the 'PUT' method instead of 'POST'.
             */
            submitFormPut: function (onSuccess, onError, onComplete) {
                core.submitFormPut(this.el, this.getValues(), onSuccess, onError, onComplete);
            }
        });

        widgets.register('form.widget-form', components.FormWidget);

        //
        // Field Types
        //

        /**
         * the 'checkbox-widget' (window.core.components.CheckboxWidget)
         * possible attributes:
         */
        components.CheckboxWidget = widgets.Widget.extend({

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                this.$typeHint = this.$('sling-post-type-hint');
                this.$deleteHint = this.$('sling-post-delete-hint');
            },

            retrieveInput: function () {
                return this.$el.is('input[type="checkbox"]')
                    ? this.$el : this.$('input[type="checkbox"]');
            },

            declareName: function (name) {
                if (name) {
                    this.$input.attr(widgets.const.attr.name, name);
                    this.$typeHint.attr(widgets.const.attr.name, name + '@TypeHint');
                    this.$deleteHint.attr(widgets.const.attr.name, name + '@Delete');
                } else {
                    this.$input.removeAttr(widgets.const.attr.name);
                    this.$typeHint.removeAttr(widgets.const.attr.name);
                    this.$deleteHint.removeAttr(widgets.const.attr.name);
                }
            },

            /**
             * returns the current validation state, always 'true' for this widget
             */
            isValid: function () {
                return true;
            },

            /**
             * returns the current value from the input field
             */
            getValue: function () {
                return this.$input.prop('checked');
            },

            /**
             * defines the (initial) value of the input field
             */
            setValue: function (value) {
                this.$input.prop('checked', value == 'false' ? false : value);
            },

            /**
             * resets the validation state and the input field value
             */
            reset: function () {
                this.$input.prop('checked', false);
            }
        });

        widgets.register('.widget.checkbox-widget', components.CheckboxWidget);

        /**
         * the 'select-buttons-widget' (window.core.components.SelectButtonsWidget)
         * possible attributes:
         */
        components.SelectButtonsWidget = widgets.Widget.extend({

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                this.$('.btn').click(_.bind(this.onSelect, this));
            },

            onSelect: function (event) {
                event.preventDefault();
                this.setValue($(event.currentTarget).attr('data-value'), true);
                return false;
            },

            getValue: function () {
                this.value = this.$('.active').attr('data-value');
                return this.value;
            },

            setValue: function (value, triggerChange) {
                this.$('.btn').removeClass('active');
                this.$('.btn[data-value="' + value + '"]').addClass('active');
                this.value = this.$('.active').attr('data-value');
                if (triggerChange) {
                    this.$el.trigger('change');
                }
            }
        });

        widgets.register('.widget.select-buttons-widget', components.SelectButtonsWidget);

        /**
         * the 'radio-group-widget' (window.core.components.RadioGroupWidget)
         * possible attributes:
         */
        components.RadioGroupWidget = widgets.Widget.extend({

            getCount: function () {
                return this.$('input[type="radio"]').length;
            },

            getOnlyOne: function () {
                return this.getCount() == 1 ? this.$('input[type="radio"]').val() : undefined;
            },

            getValue: function () {
                this.value = this.$('input[type="radio"]:checked').val();
                return this.value;
            },

            setValue: function (value, triggerChange) {
                var $radio = this.$('input[type="radio"][value="' + value + '"]');
                $radio.prop("checked", true);
                this.getValue();
                if (triggerChange) {
                    this.$el.trigger('change');
                }
            },

            reset: function () {
                this.value = this.$('input[type="radio"]:checked').removeAttr('checked');
            }
        });

        widgets.register('.widget.radio-group-widget', components.RadioGroupWidget);

        /**
         * the 'select-widget' (window.core.components.SelectWidget)
         * possible attributes:
         */
        components.SelectWidget = widgets.Widget.extend({

            retrieveInput: function () {
                return this.$el.is('select') ? this.$el : this.$('select');
            },

            getValue: function () {
                return this.$el.val();
            },

            setValue: function (value, triggerChange) {
                this.$el.val(value);
                if (triggerChange) {
                    this.$el.trigger('change');
                }
            },

            reset: function () {
                this.$el.selectedIndex = -1;
            },

            setOptions: function (options) {
                this.$el.html('');
                if (_.isArray(options)) {
                    options.forEach(function (option) {
                        if (_.isObject(option)) {
                            this.$input.append('<option value="' + (option.value || option.key) + '">' + (option.label || option.name) + '</option>');
                        } else {
                            this.$input.append('<option>' + option + '</option>');
                        }
                    }, this);
                }
            }
        });

        widgets.register('.widget.select-widget', components.SelectWidget);

        /**
         * the 'text-field-widget' (window.core.components.TextFieldWidget)
         *
         * this is the basic class ('superclass') of all text input field based widgets; it is also usable
         * as is for normal text input fields; it implements the general validation and reset functions
         * possible attributes:
         * - data-rules: 'mandatory,unique'
         * - data-pattern: a regexp pattern (javascript) as string or in pattern notation (/.../; with flags)
         */
        components.TextFieldWidget = widgets.Widget.extend({

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                this.$textField = this.textField();
                // scan 'rules / pattern' attributes
                this.initRules(this.$textField);
                // bind change events if any validation option has been found
                if (this.rules) {
                    this.$textField.on('keyup.validate', _.bind(this.validate, this));
                    this.$textField.on('change.validate', _.bind(this.validate, this));
                }
            },

            /**
             * returns the current value from the input field
             */
            getValue: function () {
                return this.$textField.val();
            },

            /**
             * defines the (initial) value of the input field
             */
            setValue: function (value, triggerChange) {
                var currentValue = this.$textField.val();
                if ('' + currentValue != '' + value) {
                    this.$textField.val(value);
                    if (triggerChange) {
                        this.$textField.trigger('change');
                    }
                }
            },

            /**
             * sets the focus on the textfield
             */
            focus: function () {
                this.$textField.focus();
            },

            /**
             * selects the complete text of textfield
             */
            selectAll: function () {
                this.$textField.select();
            },

            /**
             * retrieves the input field to use (for redefinition in more complex widgets)
             */
            textField: function () {
                return this.$el.is('input') ? this.$el : this.$('input');
            },

            /**
             * validates the current value using the 'rules' and the 'pattern' if present
             */
            validate: function (alertMethod) {
                this.valid = true;
                // check only if this field has a 'name' (included in a form) and is visible
                // prevent from validation check if the 'name' is removed or the class contains 'hidden'
                if (!this.$el.hasClass('hidden') && this.$textField.prop('name')) {
                    var value = this.getValue();
                    if (this.rules) {
                        if (this.rules.mandatory) {
                            // check for a defined and not blank value
                            var valid = this.valid = (value !== undefined &&
                            (this.rules.blank || value.trim().length > 0));
                            if (!valid) {
                                this.alert(alertMethod, 'danger', '', 'value is mandatory');
                            }
                        }
                        if (this.valid && this.rules.pattern) {
                            // check pattern only if not blank (blank is valid if allowed explicitly)
                            var valid = this.valid = (this.rules.blank && (!value || value.trim().length < 1))
                                || this.rules.pattern.test(value);
                            if (!valid) {
                                this.alert(alertMethod, 'danger', '',
                                    this.rules.patternHint || "value doesn't match pattern", this.rules.pattern);
                            }
                        }
                    }
                    // the extension hook for further validation in 'subclasses'
                    if (this.valid && _.isFunction(this.extValidate)) {
                        this.valid = this.extValidate(value);
                    }
                    if (this.valid) {
                        this.$textField.closest('.form-group').removeClass('has-error');
                    } else {
                        this.$textField.closest('.form-group').addClass('has-error');
                    }
                }
                return this.valid;
            },

            /**
             * resets the validation state and the input field value
             */
            reset: function () {
                this.valid = undefined;
                this.$textField.closest('.form-group').removeClass('has-error');
                this.$textField.val(undefined);
            }
        });

        widgets.register('.widget.text-field-widget', components.TextFieldWidget);

        /**
         * the 'text-field-widget' (window.core.components.TextFieldWidget)
         *
         * this is the basic class ('superclass') of all text input field based widgets; it is also usable
         * as is for normal text input fields; it implements the general validation and reset functions
         * possible attributes:
         * - data-rules: 'mandatory'
         * - data-pattern: a regexp pattern (javascript) as string or in pattern notation (/.../; with flags)
         */
        components.TextAreaWidget = widgets.Widget.extend({

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                // scan 'rules / pattern' attributes
                this.initRules(this.$input);
                // bind change events if any validation option has been found
                if (this.rules) {
                    this.$input.on('keyup.validate', _.bind(this.validate, this));
                    this.$input.on('change.validate', _.bind(this.validate, this));
                }
            },

            retrieveInput: function () {
                return this.$el.is('textarea') ? this.$el : this.$('textarea');
            },

            /**
             * returns the current value from the input field
             */
            getValue: function () {
                return this.$input[0].value;
            },

            /**
             * defines the (initial) value of the input field
             */
            setValue: function (value, triggerChange) {
                var currentValue = this.$input.text();
                if ('' + currentValue != '' + value) {
                    this.$input[0].value = value;
                    if (triggerChange) {
                        this.$input.trigger('change');
                    }
                }
            },

            /**
             * sets the focus on the textfield
             */
            focus: function () {
                this.$input.focus();
            },

            /**
             * selects the complete text of textfield
             */
            selectAll: function () {
                this.$input.select();
            },

            /**
             * validates the current value using the 'rules' and the 'pattern' if present
             */
            validate: function () {
                this.valid = true;
                // check only if this field has a 'name' (included in a form) and is visible
                // prevent from validation check if the 'name' is removed or the class contains 'hidden'
                if (!this.$el.hasClass('hidden') && this.$input.prop('name')) {
                    var value = this.getValue();
                    if (this.rules) {
                        var rules = this.rules;
                        if (rules.mandatory) {
                            // check for a defined and not blank value
                            this.valid = (value !== undefined &&
                            (this.rules.blank || value.trim().length > 0));
                        }
                    }
                    // the extension hook for further validation in 'subclasses'
                    if (this.valid && _.isFunction(this.extValidate)) {
                        this.valid = this.extValidate(value);
                    }
                    if (this.valid) {
                        this.$input.closest('.form-group').removeClass('has-error');
                    } else {
                        this.$input.closest('.form-group').addClass('has-error');
                    }
                }
                return this.valid;
            },

            /**
             * resets the validation state and the input field value
             */
            reset: function () {
                this.valid = undefined;
                this.$input.closest('.form-group').removeClass('has-error');
                this.$input.text('');
            }
        });

        widgets.register('.widget.text-area-widget', components.TextAreaWidget);

        /**
         * the 'path-widget' (window.core.components.PathWidget)
         *
         * the widget behaviour to extend an input or an input group to select repository path values
         * - adds a typeahead function for the last path segment during input on the input element
         * - adds a select path dialog to select the path in a tree view to a '.select' button if present
         * possible attributes:
         * - data-root: defines the root path for the path retrieval
         */
        components.PathWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                // retrieve element attributes
                this.dialogTitle = this.$el.attr('title');
                this.dialogLabel = this.$el.data('label');
                this.rootPath = this.$el.data('root') || '/';
                this.filter = this.$el.data('filter');
                // switch off the browsers autocomplete function (!)
                this.$textField.attr('autocomplete', 'off');
                // add typeahead function to the input field
                this.$textField.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        // ensure that query is of a valid path pattern
                        if (query.indexOf('/') === 0) {
                            core.getJson('/bin/cpm/nodes/node.typeahead.json' + query, function (data) {
                                callback(data);
                            });
                        }
                    },
                    // custom matcher to check last name in path only
                    matcher: function (item) {
                        var pattern = /^(.*\/)([^\/]*)$/.exec(this.query);
                        return item.match('.*' + pattern[2] + '.*');
                    },
                    // the custom highlighter to mark the name pattern in the last segment
                    highlighter: function (item) {
                        var pattern = /^(.*\/)([^\/]*)$/.exec(this.query);
                        var splitted = new RegExp('^(.*)' + pattern[2] + '(.*)?').exec(item);
                        return splitted[1] + '<b>' + pattern[2] + '</b>' + (splitted[2] || '');
                    }
                });
                // set up '.select' button if present
                this.$selectButton = this.$('button.select');
                if (this.$selectButton.length > 0) {
                    this.$selectButton.on('click', _.bind(this.selectPath, this));
                }
            },

            /**
             * the callback for the '.select' button opens set dialog with the tree view
             */
            selectPath: function (event) {
                var selectDialog = core.getView('#path-select-dialog', components.SelectPathDialog);
                selectDialog.setTitle(this.dialogTitle);
                selectDialog.setLabel(this.dialogLabel);
                selectDialog.setRootPath(this.rootPath);
                selectDialog.setFilter(this.filter);
                selectDialog.show(_.bind(function () {
                        this.getPath(_.bind(selectDialog.setValue, selectDialog));
                    }, this),
                    _.bind(function () {
                        this.setPath(selectDialog.getValue());
                    }, this));
            },

            /**
             * calls the 'callback' with the current path value (possibly after a path retrieval); extension
             * hook for more complex path retrieval - performs the callback with the current value here
             */
            getPath: function (callback) {
                callback(this.getValue());
            },

            /**
             * stores the value for a selected path; extension hook for different path based values
             */
            setPath: function (path) {
                this.setValue(path);
            }
        });

        widgets.register('.widget.path-widget', components.PathWidget);

        /**
         * the 'reference-widget' (window.core.components.ReferenceWidget)
         */
        components.ReferenceWidget = components.PathWidget.extend({

            initialize: function (options) {
                components.PathWidget.prototype.initialize.apply(this, [options]);
                if (!this.filter) {
                    this.filter = 'referenceable';
                }
            },

            /**
             * extension - retrieves the path by the current reference value and
             * calls the 'callback' with the result
             */
            getPath: function (callback) {
                if (!this.path) { // retrieve the path if not path value is cached
                    this.retrievePath(_.bind(function (path) {
                        this.path = path; // caches the path value
                        callback(path);
                    }, this));
                } else {
                    callback(this.path); // uses the cached path value
                }
            },

            /**
             * extension - determines the reference for the path as the new value
             */
            setPath: function (path) {
                this.path = undefined; // reset the possibly cached path value
                this.retrieveReference(path);
            },

            /**
             * retrieves the referenc for the path and stores this reference as value
             */
            retrieveReference: function (path) {
                core.getJson('/bin/cpm/nodes/node.tree.json' + path, _.bind(function (data) {
                    this.path = path;
                    this.setValue(data.uuid ? data.uuid : data.id);
                }, this));
            },

            /**
             * retrieves the path for the current reference value and calls the 'callback' with the result
             */
            retrievePath: function (callback) {
                var reference = this.getValue();
                if (reference) {
                    core.getJson('/bin/cpm/nodes/node.reference.json/' + reference, function (data) {
                        callback(data.path);
                    });
                }
            }
        });

        widgets.register('.widget.reference-widget', components.ReferenceWidget);

        /**
         * the 'number-field-widget' (window.core.components.NumberFieldWidget)
         * possible attributes:
         */
        components.NumberFieldWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                var dataOptions = this.$el.data('options');
                if (dataOptions) {
                    var values = dataOptions.split(':');
                    if (values.length > 0) options.minValue = values[0];
                    if (values.length > 1) options.stepSize = values[1];
                    if (values.length > 2) options.maxValue = values[2];
                }
                this.minValue = Number(options.minValue || 0);
                this.stepSize = Number(options.stepSize || 1);
                this.maxValue = options.maxValue ? Number(options.maxValue) : undefined;
                this.$('.increment').click(_.bind(this.increment, this));
                this.$('.decrement').click(_.bind(this.decrement, this));
                this.$textField.on('change.number', _.bind(this.onChange, this));
            },

            setValue: function (value, triggerChange) {
                if (value) {
                    var val = Number(value);
                    if (this.minValue !== undefined && val < this.minValue) {
                        value = this.minValue;
                    }
                    if (this.maxValue !== undefined && val > this.maxValue) {
                        value = this.maxValue;
                    }
                }
                components.TextFieldWidget.prototype.setValue.apply(this, [value, triggerChange]);
            },

            onChange: function () {
                this.setValue(this.getValue()); // filter new value for number restrictions
            },

            increment: function () {
                if (this.stepSize) {
                    this.setValue(parseInt(this.getValue()) + this.stepSize, true);
                }
            },

            decrement: function () {
                if (this.stepSize) {
                    this.setValue(parseInt(this.getValue()) - this.stepSize, true);
                }
            },

            extValidate: function (value) {
                var valid = true;
                if (valid && this.minValue !== undefined) {
                    valid = (value >= this.minValue);
                }
                if (valid && this.maxValue !== undefined) {
                    valid = (value <= this.maxValue);
                }
                return valid;
            }
        });

        widgets.register('.widget.number-field-widget', components.NumberFieldWidget);

        /**
         * the 'date-time-widget' (window.core.components.DateTimeWidget)
         * possible attributes:
         */
        components.DateTimeWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                this.$el.datetimepicker({
                    locale: 'en',
                    format: 'YYYY-MM-DD HH:mm:ss',
                    //format: 'DD.MM.YYYY HH:mm:ss',
                    extraFormats: [
                        'YY-MM-DD',
                        'YY-MM-DD HH:mm',
                        'YY-MM-DD HH:mm ZZ',
                        'YY-MM-DD HH:mm:ss',
                        'YY-MM-DD HH:mm:ss ZZ',
                        'YYYY-MM-DD',
                        'YYYY-MM-DD HH:mm',
                        'YYYY-MM-DD HH:mm ZZ',
                        'YYYY-MM-DD HH:mm:ss',
                        'YYYY-MM-DD HH:mm:ss ZZ',
                        'DD.MM.YY',
                        'DD.MM.YY HH:mm',
                        'DD.MM.YY HH:mm ZZ',
                        'DD.MM.YY HH:mm:ss',
                        'DD.MM.YY HH:mm:ss ZZ',
                        'DD.MM.YYYY',
                        'DD.MM.YYYY HH:mm',
                        'DD.MM.YYYY HH:mm ZZ',
                        'DD.MM.YYYY HH:mm:ss',
                        'DD.MM.YYYY HH:mm:ss ZZ'
                    ],
                    calendarWeeks: true,
                    showTodayButton: true,
                    showClear: true,
                    showClose: true
                });
            },

            /**
             * defines the (initial) value of the input field
             */
            setValue: function (value, triggerChange) {
                this.$el.data('DateTimePicker').date(value ? new Date(value) : undefined);
                this.validate();
                if (triggerChange) {
                    this.$el.trigger('change');
                }
            }
        });

        widgets.register('.widget.date-time-widget', components.DateTimeWidget);

        /**
         * the 'file-upload-widget' (window.core.components.FileUploadWidget)
         * possible attributes:
         * - data-options: 'hidePreview' (no file preview), 'showUpload' (the direct upload button)
         */
        components.FileUploadWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                var dataOptions = this.$el.data('options');
                if (dataOptions) {
                    if (dataOptions.indexOf('hidePreview') >= 0) {
                        options.showPreview = false;
                        options.showUploadedThumbs = false;
                    }
                    if (dataOptions.indexOf('showUpload') >= 0) {
                        options.showUpload = true;
                    }
                }
                var dataType = this.$el.data('type');
                if (dataType) {
                    options.fileType = dataType;
                }
                this.whatever = this.$textField.fileinput({
                    showPreview: options.showPreview === undefined ? true : options.showPreview,
                    showUpload: options.showUpload || false,
                    fileType: options.fileType || "any"
                });
                this.$widget = this.$el.closest('.file-input-new');
                this.$inputCaption = this.$widget.find('.kv-fileinput-caption');
            },

            /**
             * defines the (initial) value of the input field
             */
            setValue: function (value) {
            },

            grabFocus: function () {
                this.$inputCaption.focus();
            },

            /**
             * resets the validation state and the input field value
             */
            reset: function () {
                this.$textField.fileinput('clear');
            }
        });

        widgets.register('.widget.file-upload-widget', components.FileUploadWidget);

        /**
         * the 'property-name-widget' (window.core.components.RepositoryNameWidget)
         */
        components.PropertyNameWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                // switch off the browsers autocomplete function (!)
                //this.$textField.attr('autocomplete', 'off');
                // add typeahead function to the input field
                this.$textField.typeahead({
                    minLength: 1,
                    source: [
                        'jcr:data',
                        'jcr:description',
                        'jcr:lastModified',
                        'jcr:lastModifiedBy',
                        'jcr:mixinTypes',
                        'jcr:primaryType',
                        'jcr:title',
                        'sling:redirect',
                        'sling:resourceType'
                    ]
                });
            }
        });

        widgets.register('.widget.property-name-widget', components.PropertyNameWidget);

        /**
         * the 'repository-name-widget' (window.core.components.RepositoryNameWidget)
         */
        components.RepositoryNameWidget = components.TextFieldWidget.extend({

            nameChanged: function (name, contextWidget) {
                // set 'subtypes' of name for useful typeahead
                if ('jcr:primaryType' == name) {
                    contextWidget.setWidgetType.apply(contextWidget, ['jcr-primaryType']);
                } else if ('jcr:mixinTypes' == name) {
                    contextWidget.setWidgetType.apply(contextWidget, ['jcr-mixinTypes']);
                }
            }
        });

        widgets.register('.widget.repository-name-widget', components.RepositoryNameWidget);

        /**
         * the 'primary-type-widget' (window.core.components.PrimaryTypeWidget)
         */
        components.PrimaryTypeWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                // switch off the browsers autocomplete function (!)
                this.$textField.attr('autocomplete', 'off');
                // add typeahead function to the input field
                this.$textField.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        core.ajaxGet('/bin/cpm/core/system.primaryTypes.json', {
                            query: query
                        }, function (data) {
                            callback(data);
                        })
                    }
                });
            }
        });

        widgets.register('.widget.primary-type-widget', components.PrimaryTypeWidget);

        /**
         * the 'mixin-type-widget' (window.core.components.MixinTypeWidget)
         */
        components.MixinTypeWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                // switch off the browsers autocomplete function (!)
                this.$textField.attr('autocomplete', 'off');
                // add typeahead function to the input field
                this.$textField.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        core.ajaxGet('/bin/cpm/core/system.mixinTypes.json', {
                            query: query
                        }, function (data) {
                            callback(data);
                        })
                    }
                });
            }
        });

        widgets.register('.widget.mixin-type-widget', components.MixinTypeWidget);

    })(core.components, window.widgets);

})(window.core);
