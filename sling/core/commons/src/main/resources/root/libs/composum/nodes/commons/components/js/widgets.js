/**
 *
 *
 */
(function (window) {
    'use strict';

    window.widgets = {

        const: {
            css: {
                base: 'widget',
                selector: {
                    general: '.widget',
                    prefix: '.widget.',
                    form: 'form.form-widget',
                    group: '.form-group',
                    label: 'label .label-text'
                }
            },
            attr: {
                name: 'name'
            },
            tag: {
                input: ['input', 'select', 'textarea']
            }
        },

        registered: {},

        /**
         * Register a widget type by a DOM selector as key for this type.
         * @param selector    the jQuery selector to find the widgets instances
         * @param widgetClass the widget implementation class (required)
         * @param options     additional options (see richtext-widget for example)
         */
        register: function (selector, widgetClass, options) {
            widgets.registered[selector] = _.extend({
                selector: selector,
                widgetClass: widgetClass
            }, options);
        },

        /**
         * Set up all components of a DOM element; add a view at each element marked with
         * a widget css class. Uses the 'core.getView()' function to add the View to the
         * DOM element itself to avoid multiple View instances for one DOM element.
         */
        setUp: function (root) {
            var $root = root ? $(root) : $(document);
            _.keys(widgets.registered).forEach(function (selector) {
                $root.find(selector).each(function () {
                    var widget = widgets.registered[selector];
                    core.getView(this, widget.widgetClass);
                });
            });
        },

        /**
         * applies the abstract 'option' (must be a function) to all widgets which have such
         * an option registered and are matching to the widgets selector in the 'root's context
         * used for example
         *  - by the multi-form-widget to apply the 'afterClone' option after item creation (cloning)
         */
        apply: function (root, option) {
            _.keys(widgets.registered).forEach(function (selector) {
                var widget = widgets.registered[selector];
                var method = widget[option];
                if (_.isFunction(method)) {
                    var $root = $(root);
                    if ($root.is(widget.selector)) {
                        method.apply(root);
                    } else {
                        $root.find(widget.selector).each(function () {
                            method.apply(this);
                        });
                    }
                }
            });
        },

        /**
         *
         * #abstract
         */
        Widget: Backbone.View.extend({

            initialize: function (options) {
                this.$input = this.retrieveInput();
                this.name = this.retrieveName();
                this.form = this.retrieveForm();
            },

            retrieveInput: function () {
                var $inputEl = [];
                for (var i = 0; $inputEl.length === 0 && i < widgets.const.tag.input.length; i++) {
                    $inputEl = this.$el.is(widgets.const.tag.input[i]) ? this.$el
                        : this.$(widgets.const.tag.input[i] + ':not([type="hidden"])');
                }
                return $inputEl;
            },

            retrieveName: function () {
                return this.$input.attr(widgets.const.attr.name);
            },

            retrieveLabel: function () {
                var c = widgets.const.css.selector;
                var $label = this.$el.closest(c.group).find(c.label);
                return $label.length === 1 ? $label.text().trim() : this.retrieveName();
            },

            declareName: function (name) {
                if (name) {
                    this.$input.attr(widgets.const.attr.name, name);
                } else {
                    this.$input.removeAttr(widgets.const.attr.name);
                }
            },

            retrieveForm: function () {
                var $form = this.$el.closest(widgets.const.css.selector.form);
                return $form.length > 0 ? $form[0].view : undefined;
            },

            /**
             * #abstract
             * @returns {undefined}
             */
            getValue: function () {
                return undefined;
            },

            /**
             * @returns the - probably prepared - value for the input validation
             */
            getValueForValidation: function(){
                return this.getValue();
            },

            /**
             * #abstract
             */
            setValue: function (value) {
            },

            /**
             * #default
             */
            setDefaultValue: function (value) {
            },

            /**
             * #abstract
             */
            reset: function () {
            },

            /**
             * returns the current validation state, calls 'validate' if no state is present
             */
            isValid: function (alertMethod) {
                if (this.valid === undefined) {
                    this.valid = _.isFunction(this.validate)
                        ? this.validate(alertMethod)
                        : true;
                }
                this.alertFlush(alertMethod);
                return this.valid;
            },

            validationReset: function () {
                this.valid = undefined;
                this.alertMessage = undefined;
            },

            /**
             * validates the current value using the 'rules' and the 'pattern' if present
             */
            validate: function (alertMethod) {
                this.valid = true;
                // check only if this field has a 'name' (included in a form) and is visible
                // prevent from validation check if the 'name' is removed or the class contains 'hidden'
                if (!this.$el.hasClass('hidden') && this.retrieveName()) {
                    var value = this.getValueForValidation();
                    if (this.rules) {
                        var valid;
                        if (this.valid && this.rules.pattern) {
                            // check pattern only if not blank (blank is valid if allowed explicitly)
                            valid = this.valid = (this.rules.blank && (!value || value.trim().length < 1))
                                || this.rules.pattern.test(value);
                            if (!valid) {
                                this.alert(alertMethod, 'danger', '',
                                    this.rules.patternHint || core.i18n.get("value doesn't match pattern"),
                                    ('' + this.rules.pattern).replace(/^\//, '').replace(/\/$/, ''));
                            }
                        }
                        if (this.valid && this.rules.required) {
                            // check for a defined and not blank value
                            valid = this.valid = (value !== undefined &&
                                (this.rules.blank || value.trim().length > 0));
                            if (!valid) {
                                this.alert(alertMethod, 'danger', '', core.i18n.get('value is required'));
                            }
                        }
                    }
                    // the extension hook for further validation in 'subclasses'
                    if (this.valid && _.isFunction(this.extValidate)) {
                        this.valid = this.extValidate(value);
                    }
                    if (this.valid) {
                        this.$el.closest('.form-group').removeClass('has-error');
                    } else {
                        this.$el.closest('.form-group').addClass('has-error');
                    }
                }
                return this.valid;
            },

            initRules: function ($element) {
                if (!$element) {
                    $element = this.$el;
                }
                this.label = $element.data('label') || this.retrieveLabel();
                // scan 'data-pattern' attribute
                var pattern = $element.data('pattern');
                if (pattern) {
                    try {
                        this.rules = _.extend(this.rules || {}, {
                            pattern: pattern.indexOf('/') === 0
                                // use '/.../ig' to specify pattern and flags
                                ? eval(pattern)
                                // pure strings can not have additional flags...
                                : new RegExp(pattern)
                        });
                    } catch (ex) {
                        core.log.error("widget[" + this.name + "]: " + ex);
                    }
                    var patternHint = $element.data('pattern-hint');
                    if (patternHint) {
                        this.rules.patternHint = patternHint;
                    }
                }
                // scan 'data-rules' attribute
                var rules = $element.data('rules');
                if (rules) {
                    this.rules = _.extend(this.rules || {}, {
                        required: rules.indexOf('required') >= 0 || rules.indexOf('mandatory') >= 0,
                        blank: rules.indexOf('blank') >= 0,
                        unique: rules.indexOf('unique') >= 0
                    });
                }
            },

            grabFocus: function () {
                this.$input.focus();
            },

            alert: function (alertMethod, type, label, message, hint) {
                if (_.isFunction(alertMethod)) {
                    alertMethod(type, label || this.label, message, hint);
                } else {
                    this.alertMessage = {
                        type: type,
                        label: label,
                        message: message,
                        hint: hint
                    }
                }
            },

            /**
             * print out a probably delayed message
             */
            alertFlush: function (alertMethod) {
                if (_.isFunction(alertMethod) && this.alertMessage) {
                    alertMethod(this.alertMessage.type, this.alertMessage.label || this.label,
                        this.alertMessage.message, this.alertMessage.hint);
                    this.alertMessage = undefined;
                }
            }
        })
    };

    /**
     * register the 'hidden' input as a widget to add the widgets behaviour to such hidden values
     */
    window.widgets.register('.widget.hidden-widget', window.widgets.Widget);

})(window);
