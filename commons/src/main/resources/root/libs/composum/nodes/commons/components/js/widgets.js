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
                    form: 'form.widget-form',
                    group: '.form-group',
                    labeltext: 'label .label-text',
                    label: 'label'
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
         * a Widget implements the beaviour of a value editing DOM element (maybe as part of a form)
         * the DOM element is not rendered by the Widget; a Widget is assigned to an existing appropriate
         * DOM element rendered by a template on the server side (by a Sling component)
         * @abstract this is a general 'interface' and basic 'class' to implement a form behaviour
         * @event 'changed' a value changed event (a jQuery 'change' is also triggered but
         * not only on value changes, widget state changes can trigger a 'change' also)
         */
        Widget: Backbone.View.extend({

            /**
             * @protected
             */
            initialize: function (options) {
                this.$input = this.retrieveInput();
                this.rules = {};
                this.name = this.retrieveName();
                this.form = this.retrieveForm();
                if (this.form && _.isFunction(this.form.registerWidget)) {
                    this.form.registerWidget(this);
                }
                this.initChangeEvent();
            },

            /**
             * @protected
             */
            retrieveInput: function () {
                var $inputEl = [];
                for (var i = 0; $inputEl.length === 0 && i < widgets.const.tag.input.length; i++) {
                    $inputEl = this.$el.is(widgets.const.tag.input[i]) ? this.$el
                        : this.$(widgets.const.tag.input[i] + ':not([type="hidden"])');
                }
                return $inputEl;
            },

            /**
             * @protected
             */
            retrieveName: function () {
                return this.$input.attr(widgets.const.attr.name);
            },

            /**
             * @protected
             */
            retrieveLabel: function () {
                var c = widgets.const.css.selector;
                var $label = this.$el.closest(c.group).find(c.labeltext);
                if ($label.length < 1) {
                    $label = this.$el.closest(c.group).find(c.label);
                }
                return $label.length === 1 ? $label.text().trim() : this.retrieveName();
            },

            /**
             * @public sets the input 'name' attribute - the name of the property to edit
             * @param name the name; removes the name if (!name)
             */
            declareName: function (name) {
                if (name) {
                    this.$input.attr(widgets.const.attr.name, name);
                } else {
                    this.$input.removeAttr(widgets.const.attr.name);
                }
            },

            /**
             * @protected
             */
            retrieveForm: function () {
                var $form = this.$el.closest(widgets.const.css.selector.form);
                return $form.length > 0 ? $form[0].view : undefined;
            },

            /**
             * @public
             * @abstract
             * @returns {undefined}
             */
            getValue: function () {
                return undefined;
            },

            /**
             * @public
             * @returns the - probably prepared - value for the input validation
             */
            getValueForValidation: function () {
                return this.getValue();
            },

            /**
             * @public
             * @abstract depends on the concrete widget
             */
            setValue: function (value, triggerChange) {
            },

            /**
             * @public
             * @default empty; the default value is the value used if no value is present (no persistent value)
             */
            setDefaultValue: function (value) {
            },

            /**
             * @public registers a value 'changed' handler
             * @param key a unique id of the registrator to separate the various handlers
             */
            changed: function (key, handler) {
                this.$el.off('changed.' + key).on('changed.' + key, handler);
            },

            /**
             * @protected triggers a value 'changed' event if jQuery is triggering a 'change'
             * this can be modified by a widget to filter change events, e.g. on internal state changes
             */
            onWidgetChange: function (event, value) {
                this.$el.trigger('changed', [value ? value : this.getValue()]);
            },

            /**
             * registers the widgets value change handler which is mapping 'change' events to 'changed' events
             * @protected
             */
            initChangeEvent: function () {
                this.$el.on('change.Widget', _.bind(this.onWidgetChange, this));
            },

            /**
             * @abstract reset the widget state and value
             */
            reset: function () {
            },

            /**
             * @public
             * @return the current validation state, calls 'validate' if no state is present
             * @param alertMethod (type, label, message, hint)
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
             * @public
             * validates the current value using the 'rules' and the 'pattern' if present
             * @return the current validation state, calls 'validate' if no state is present
             * @param alertMethod (type, label, message, hint)
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
                                if (this.rules.patternHint) {
                                    this.alert(alertMethod, 'danger', '',
                                        this.rules.patternHint, this.patternHint(this.rules.pattern));
                                } else {
                                    core.i18n.get("value doesn't match pattern", _.bind(function (text) {
                                        this.alert(alertMethod, 'danger', '',
                                            text, this.patternHint(this.rules.pattern));
                                    }, this));
                                }
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

            /**
             * @public
             * @return the 'disabled' state of the widget
             */
            isDisabled: function () {
                return (this.rules && this.rules.disabled) || (this.$input && this.$input.prop('disabled'));
            },

            /**
             * @public sets the disabled state
             * @param disabled boolean
             */
            setDisabled: function (disabled) {
                this.rules = _.extend(this.rules || {}, {
                    disabled: disabled
                });
                if (this.$input) {
                    this.$input.prop('disabled', disabled);
                }
            },

            /**
             * @public sets the focus to the widgets input element
             */
            grabFocus: function () {
                this.$input.focus();
            },

            /**
             * @protected
             * @initialize should be called during initialization; is not part of the Widget.initialize()
             * @param $element optional; the element to use to get the data attributes
             */
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
                        unique: rules.indexOf('unique') >= 0,
                        disabled: rules.indexOf('disabled') >= 0 || (this.$input && this.$input.prop('disabled'))
                    });
                }
            },

            /**
             * @protected
             * @param alertMethod (type, label, message, hint)
             */
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
             * @protected
             * print out a probably delayed message
             */
            alertFlush: function (alertMethod) {
                if (_.isFunction(alertMethod) && this.alertMessage) {
                    alertMethod(this.alertMessage.type, this.alertMessage.label || this.label,
                        this.alertMessage.message, this.alertMessage.hint);
                    this.alertMessage = undefined;
                }
            },

            /**
             * @protected
             * @return a 'readable' text derived from the regex pattern used vor validation
             */
            patternHint: function (pattern) {
                return ('' + pattern)
                    .replace(/^\//, '').replace(/\/$/, '')
                    .replace(/^[^]/, '').replace(/[$]$/, '');
            }
        })
    };

    /**
     * register the 'hidden' input as a widget to add the widgets behaviour to such hidden values
     */
    window.widgets.register('.widget.hidden-widget', window.widgets.Widget);

})(window);
