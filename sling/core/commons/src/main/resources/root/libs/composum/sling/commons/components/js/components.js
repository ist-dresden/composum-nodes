/**
 *
 *
 */
(function(core) {
    'use strict';

    core.components = core.components || {};

(function(components) {

    //
    // Form
    //

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

        /**
         * the widgets 'isValid' always performs a 'validation' of the form
         */
        isValid: function() {
            return this.validate();
        },

        /**
         * the validation calls the 'isValid' function of each widget in the form;
         * the class of the form signals the result ('valid-form / 'invalid-form')
         */
        validate: function() {
            var valid = true;
            this.$('.widget').each(function(){
                if (this.view) {
                    if (_.isFunction (this.view.isValid)) {
                        // check each widget independent from the current result
                        valid = (this.view.isValid.apply(this.view) && valid);
                    }
                }
            });
            if (valid) {
                this.$el.removeClass('invalid-form');
                this.$el.addClass('valid-form');
            } else {
                this.$el.removeClass('valid-form');
                this.$el.addClass('invalid-form');
            }
            return valid;
        },

        /**
         * a reset of a form resets all widgets calling their 'reset' function
         */
        reset: function() {
            this.$('.widget').each(function(){
                if (this.view) {
                    if (_.isFunction (this.view)) {
                        this.view.reset.apply(this.view);
                    }
                }
            });
        }
    });

    //
    // Field Types
    //

    /**
     * the 'checkbox-widget' (window.core.components.CheckboxWidget)
     * possible attributes:
     */
    components.CheckboxWidget = Backbone.View.extend({

        initialize: function(options) {
            this.$checkkox = this.$el.is('input[type="checkbox"]')
                ? this.$el : this.$('input[type="checkbox"]');
        },

        /**
         * returns the current validation state, always 'true' for this widget
         */
        isValid: function() {
            return true;
        },

        /**
         * returns the current value from the input field
         */
        getValue: function() {
            return this.$checkkox.prop('checked');
        },

        /**
         * defines the (initial) value of the input field
         */
        setValue: function(value) {
            this.$checkkox.prop('checked',value);
        },

        /**
         * resets the validation state and the input field value
         */
        reset: function() {
            this.$checkkox.prop('checked',false);
        }
    });

    /**
     * the 'select-buttons-widget' (window.core.components.SelectButtonsWidget)
     * possible attributes:
     */
    components.SelectButtonsWidget = Backbone.View.extend({

        initialize: function(options) {
            this.$('.btn').click(_.bind (this.onSelect, this));
        },

        onSelect: function(event) {
            event.preventDefault();
            this.setValue($(event.currentTarget).attr('data-value'), true);
            return false;
        },

        getValue: function() {
            this.value = this.$('.active').attr('data-value');
            return this.value;
        },

        setValue: function(value, triggerChange) {
            this.$('.btn').removeClass('active');
            this.$('.btn[data-value="'+value+'"]').addClass('active');
            this.value = this.$('.active').attr('data-value');
            if (triggerChange) {
                this.$el.trigger('change');
            }
        }
    });

    /**
     * the 'radio-group-widget' (window.core.components.RadioGroupWidget)
     * possible attributes:
     */
    components.RadioGroupWidget = Backbone.View.extend({

        getValue: function() {
            this.value = this.$('input[type="radio"]:checked').val();
            return this.value;
        },

        setValue: function(value, triggerChange) {
            var $radio = this.$('input[type="radio"][value="'+value+'"]');
            $radio.attr('checked','checked');
            this.getValue();
            if (triggerChange) {
                this.$el.trigger('change');
            }
        },

        reset: function() {
            this.value = this.$('input[type="radio"]:checked').removeAttr('checked');
        }
    });

    /**
     * the 'combo-box-widget' (window.core.components.ComboBoxWidget)
     * possible attributes:
     */
    components.ComboBoxWidget = Backbone.View.extend({

        initialize: function(options) {
        },

        getValue: function() {
            return this.$el.val();
        },

        setValue: function(value, triggerChange) {
            this.$el.val(value);
            if (triggerChange) {
                this.$el.trigger('change');
            }
        },

        reset: function() {
            this.$el.selectedIndex = -1;
        }
    });

    /**
     * the 'text-field-widget' (window.core.components.TextFieldWidget)
     *
     * this is the basic class ('superclass') of all text input field based widgets; it is also usable
     * as is for normal text input fields; it implements the general validation and reset functions
     * possible attributes:
     * - data-rules: 'mandatory'
     * - data-pattern: a regexp pattern (javascript) as string or in pattern notation (/.../; with flags)
     */
    components.TextFieldWidget = Backbone.View.extend({

        initialize: function(options) {
            this.$textField = this.textField();
            // scan 'data-pattern' attribute
            var pattern = this.$textField.attr('data-pattern');
            if (pattern) {
                if (pattern.indexOf('/') === 0) { // use this to specify flags
                    this.$textField.pattern = eval(pattern);
                } else { // pure strings can not have additional flags...
                    this.$textField.pattern = new RegExp(pattern);
                }
            }
            // scan 'data-rules' attribute
            var rules = this.$textField.attr('data-rules');
            if (rules) {
                this.$textField.rules = {
                    mandatory: rules.indexOf('mandatory' >= 0)
                }
            }
            // bind change events if any validation option has been found
            if (this.$textField.pattern || this.$textField.rules) {
                this.$textField.on('keyup.validate', _.bind(this.validate, this));
                this.$textField.on('change.validate', _.bind(this.validate, this));
            }
        },

        /**
         * returns the current value from the input field
         */
        getValue: function() {
            return this.$textField.val();
        },

        /**
         * defines the (initial) value of the input field
         */
        setValue: function(value, triggerChange) {
            var currentValue = this.$textField.val();
            if ('' + currentValue != '' + value) {
                this.$textField.val(value);
                if (triggerChange) {
                    this.$textField.trigger('change');
                }
            }
        },

        /**
         * retrieves the input field to use (for redefinition in more complex widgets)
         */
        textField: function() {
            return this.$el.is('input') ? this.$el : this.$('input');
        },

        /**
         * returns the current validation state, calls 'validate' if not state is present
         */
        isValid: function() {
            if (this.valid === undefined) {
                this.validate();
            }
            return this.valid;
        },

        /**
         * validates the current value using the 'rules' and the 'pattern' if present
         */
        validate: function() {
            this.valid = true;
            // check only if this field has a 'name' (included in a form) and is visible
            // prevent from validation check if the 'name' is removed or the class contains 'hidden'
            if (!this.$el.hasClass('hidden') && this.$textField.prop('name')) {
                var value = this.getValue();
                if (this.$textField.rules) {
                    var rules = this.$textField.rules;
                    if (rules.mandatory) {
                        // check for a defined and not blank value
                        this.valid = (value !== undefined && value.trim().length > 0);
                    }
                }
                if (this.valid && this.$textField.pattern) {
                    // check pattern only if not blank (blank is aöways valid if not mandatory)
                    if (value && value.trim().length > 0) {
                        this.valid = this.$textField.pattern.test(value);
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
        reset: function() {
            this.valid = undefined;
            this.$textField.closest('.form-group').removeClass('has-error');
            this.$textField.val(undefined);
        }
    });

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

        initialize: function(options) {
            components.TextFieldWidget.prototype.initialize.apply(this, [options]);
            // retrieve element attributes
            this.rootPath = this.$el.attr('data-root') || '/';
            this.filter = this.$el.attr('data-filter');
            // switch off the browsers autocomplete function (!)
            this.$textField.attr('autocomplete', 'off');
            // add typeahead function to the input field
            this.$textField.typeahead({
                minLength: 1,
                source: function (query,callback) {
                    // ensure that query is of a valid path pattern
                    if (query.indexOf('/') === 0) {
                        $.get('/bin/core/node.typeahead.json' + query, {
                            }, function(data) {
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
                    var splitted = new RegExp('^(.*)' + pattern[2] + '(.*)?').exec (item);
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
        selectPath: function(event) {
            var selectDialog = core.getView('#path-select-dialog', components.SelectPathDialog);
            selectDialog.setFilter(this.filter);
            selectDialog.show(_.bind(function(){
                    this.getPath(_.bind (selectDialog.setValue, selectDialog));
                }, this),
                    _.bind(function(){
                    this.setPath(selectDialog.getValue());
                }, this));
        },

        /**
         * calls the 'callback' with the current path value (possibly after a path retrieval); extension
         * hook for more complex path retrieval - performs the callback with the current value here
         */
        getPath: function(callback) {
            callback (this.getValue());
        },

        /**
         * stores the value for a selected path; extension hook for different path based values
         */
        setPath: function(path) {
            this.setValue(path);
        }
    });

    /**
     * the 'reference-widget' (window.core.components.ReferenceWidget)
     */
    components.ReferenceWidget = components.PathWidget.extend({

        initialize: function(options) {
            components.PathWidget.prototype.initialize.apply(this, [options]);
            if (!this.filter) {
                this.filter = 'referenceable';
            }
        },

        /**
         * extension - retrieves the path by the current reference value and
         * calls the 'callback' with the result
         */
        getPath: function(callback) {
            if (!this.path) { // retrieve the path if not path value is cached
                this.retrievePath (_.bind (function(path){
                    this.path = path; // caches the path value
                    callback (path);
                }, this));
            } else {
                callback(this.path); // uses the cached path value
            }
        },

        /**
         * extension - determines the reference for the path as the new value
         */
        setPath: function(path) {
            this.path = undefined; // reset the possibly cached path value
            this.retrieveReference(path);
        },

        /**
         * retrieves the referenc for the path and stores this reference as value
         */
        retrieveReference: function(path) {
            $.getJSON('/bin/core/node.tree.json' + path, _.bind (function(data) {
                this.path = path;
                this.setValue(data.uuid ? data.uuid : data.id);
            }, this));
        },

        /**
         * retrieves the path for the current reference value and calls the 'callback' with the result
         */
        retrievePath: function(callback) {
            var reference = this.getValue();
            if (reference) {
                $.getJSON('/bin/core/node.reference.json/' + reference, function(data) {
                    callback(data.path);
                });
            }
        }
    });

    /**
     * the 'number-field-widget' (window.core.components.NumberFieldWidget)
     * possible attributes:
     */
    components.NumberFieldWidget = components.TextFieldWidget.extend({

        initialize: function(options) {
            components.TextFieldWidget.prototype.initialize.apply(this, [options]);
            var dataOptions = this.$el.attr('data-options');
            if (dataOptions) {
                var values = dataOptions.split(':');
                if (values.length > 0) options.minValue = values[0];
                if (values.length > 1) options.stepSize = values[1];
                if (values.length > 2) options.maxValue = values[2];
            }
            this.minValue = Number(options.minValue || 0);
            this.stepSize = Number(options.stepSize || 1);
            this.maxValue = options.maxValue ? Number(options.maxValue) : undefined;
            this.$('.increment').click(_.bind (this.increment, this));
            this.$('.decrement').click(_.bind (this.decrement, this));
            this.$textField.on('change.number', _.bind (this.onChange, this));
        },

        setValue: function(value, triggerChange) {
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

        onChange: function() {
            this.setValue (this.getValue()); // filter new value for number restrictions
        },

        increment: function() {
            if (this.stepSize) {
                this.setValue(parseInt(this.getValue()) + this.stepSize, true);
            }
        },

        decrement: function() {
            if (this.stepSize) {
                this.setValue(parseInt(this.getValue()) - this.stepSize, true);
            }
        },

        extValidate: function(value) {
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

    /**
     * the 'date-time-widget' (window.core.components.DateTimeWidget)
     * possible attributes:
     */
    components.DateTimeWidget = components.TextFieldWidget.extend({

        initialize: function(options) {
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
        setValue: function(value, triggerChange) {
            this.$el.data('DateTimePicker').date(value ? new Date(value) : undefined);
            this.validate();
            if (triggerChange) {
                this.$el.trigger('change');
            }
        }
    });

    /**
     * the 'file-upload-widget' (window.core.components.FileUploadWidget)
     * possible attributes:
     * - data-options: 'hidePreview' (no file preview), 'showUpload' (the direct upload button)
     */
    components.FileUploadWidget = components.TextFieldWidget.extend({

        initialize: function(options) {
            components.TextFieldWidget.prototype.initialize.apply(this, [options]);
            var dataOptions = this.$el.attr('data-options');
            if (dataOptions) {
                if (dataOptions.indexOf('hidePreview') >= 0) {
                    options.showUpload = false;
                }
                if (dataOptions.indexOf('showUpload') >= 0) {
                    options.showUpload = true;
                }
            }
            var dataType = this.$el.attr('data-type');
            if (dataType) {
                options.fileType = dataType;
            }
            this.$textField.fileinput({
                showPreview: options.showPreview === undefined ? true : options.showPreview,
                showUpload:  options.showUpload || false,
                fileType: options.fileType || "any"
            });
        },

        /**
         * defines the (initial) value of the input field
         */
        setValue: function(value) {
        }
    });

    /**
     * the 'property-name-widget' (window.core.components.RepositoryNameWidget)
     */
    components.PropertyNameWidget = components.TextFieldWidget.extend({

       initialize: function(options) {
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

    /**
     * the 'repository-name-widget' (window.core.components.RepositoryNameWidget)
     */
    components.RepositoryNameWidget = components.TextFieldWidget.extend({

        nameChanged: function(name, contextWidget) {
            // set 'subtypes' of name for useful typeahead
            if ('jcr:primaryType' == name) {
                contextWidget.setWidgetType.apply (contextWidget, ['jcr-primaryType']);
            } else if ('jcr:mixinTypes' == name) {
                contextWidget.setWidgetType.apply (contextWidget, ['jcr-mixinTypes']);
            } else {
                // reset to default type if no useful subtype is available
                contextWidget.setType.apply (contextWidget);
            }
        }
    });

    /**
     * the 'primary-type-widget' (window.core.components.PrimaryTypeWidget)
     */
    components.PrimaryTypeWidget = components.TextFieldWidget.extend({

        initialize: function(options) {
            components.TextFieldWidget.prototype.initialize.apply(this, [options]);
             // switch off the browsers autocomplete function (!)
            this.$textField.attr('autocomplete', 'off');
            // add typeahead function to the input field
            this.$textField.typeahead({
                minLength: 1,
                source: function (query,callback) {
                    $.get('/bin/core/system.primaryTypes.json', {
                            query: query
                        }, function(data) {
                            callback(data);
                        })
                }
            });
        }
    });

    /**
     * the 'mixin-type-widget' (window.core.components.MixinTypeWidget)
     */
    components.MixinTypeWidget = components.TextFieldWidget.extend({

        initialize: function(options) {
            components.TextFieldWidget.prototype.initialize.apply(this, [options]);
             // switch off the browsers autocomplete function (!)
            this.$textField.attr('autocomplete', 'off');
            // add typeahead function to the input field
            this.$textField.typeahead({
                minLength: 1,
                source: function (query,callback) {
                    $.get('/bin/core/system.mixinTypes.json', {
                            query: query
                        }, function(data) {
                            callback(data);
                        })
                }
            });
        }
    });

})(core.components);

})(window.core);
