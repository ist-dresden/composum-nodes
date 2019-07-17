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
                this.validationReset();
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
                            // check each visible widget independent from the current result
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
                        if (this.view.$el.parent().closest(c.css.selector.item).length === 0 &&
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
                        if (this.view.$el.parent().closest(c.css.selector.item).length === 0 &&
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
             * finalize all date before the following submit (prepare data for storing)
             */
            finalize: function () {
                var c = components.const.form;
                this.$(widgets.const.css.selector.general).each(function () {
                    if (this.view) {
                        if (_.isFunction(this.view.finalize)) {
                            // prepare each widget independent
                            this.view.finalize.apply(this.view);
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
                if (!this.$input.attr('value')) {
                    this.$input.attr('value', 'true')
                }
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
                this.$input.prop('checked', value === 'false' ? false : value);
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
                // scan 'rules / pattern' attributes
                this.initRules();
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

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                // scan 'rules / pattern' attributes
                this.initRules();
            },

            getCount: function () {
                return this.$('input[type="radio"]').length;
            },

            getOnlyOne: function () {
                return this.getCount() === 1 ? this.$('input[type="radio"]').val() : undefined;
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

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                // scan 'rules / pattern' attributes
                this.initRules();
            },

            retrieveInput: function () {
                return this.$el.is('select') ? this.$el : this.$('select');
            },

            getValue: function () {
                return this.$input.val();
            },

            setValue: function (value, triggerChange) {
                this.$input.val(value);
                if (triggerChange) {
                    this.$el.trigger('change');
                }
            },

            reset: function () {
                this.$input.val(undefined);
            },

            setOptions: function (options) {
                this.$input.html('');
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
         * the behaviour of a table with rows containing a checkbox to implement a multiple selection
         */
        components.TableSelectWidget = components.SelectWidget.extend({

            initialize: function (options) {
                components.SelectWidget.prototype.initialize.apply(this, [options]);
                this.$items = this.$('tbody tr');
                this.$items.click(_.bind(this.toggleElement, this));
                this.$('thead tr input[type="checkbox"]').change(_.bind(this.toggleAll, this));
            },

            isNotEmpty: function () {
                return this.$items.length > 0;
            },

            toggleElement: function (event) {
                event.preventDefault();
                var $row = $(event.currentTarget);
                var $checkbox = $row.find('input[type="checkbox"]');
                $checkbox.prop('checked', !$checkbox.prop('checked'));
                return false;
            },

            toggleAll: function (event) {
                var $checkbox = $(event.currentTarget);
                var value = $checkbox.prop('checked');
                this.$('tbody tr input[type="checkbox"]').prop('checked', value);
                return false;
            },

            // SelectWidget

            retrieveInput: function () {
                return this.$('input[type="checkbox"]');
            },

            getValue: function () {
                var value = [];
                this.$('input[type="checkbox"]:checked').each(_.bind(function (i, el) {
                    value.push($(el).attr('value'))
                }, this));
                return value;
            },

            setValue: function (value, triggerChange) {
                if (!_.isArray(value)) {
                    value = [value];
                }
                this.$('input[type="checkbox"]').prop('checked', false);
                value.forEach(_.bind(function (val) {
                    this.$('input[type="checkbox"][value="' + val + '"]').prop('checked', true);
                }, this));
                if (triggerChange) {
                    this.$el.trigger('change');
                }
            }
        });

        widgets.register('.widget.table-select-widget', components.TableSelectWidget);

        /**
         * the 'text-field-widget' (window.core.components.TextFieldWidget)
         *
         * this is the basic class ('superclass') of all text input field based widgets; it is also usable
         * as is for normal text input fields; it implements the general validation and reset functions
         * possible attributes:
         * - data-rules: 'required,unique'
         * - data-pattern: a regexp pattern (javascript) as string or in pattern notation (/.../; with flags)
         */
        components.TextFieldWidget = widgets.Widget.extend({

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                this.$textField = this.textField();
                // scan 'rules / pattern' attributes
                this.initRules();
                var typeahead = this.typeahead(options);
                if (typeahead) {
                    // switch off the browsers autocomplete function (!)
                    if (typeahead.autocomplete !== 'auto') {
                        this.$textField.attr('autocomplete', typeahead.autocomplete || 'off');
                    }
                    this.$textField.typeahead(typeahead);
                }
                // bind change events if any validation option has been found
                if (this.rules) {
                    this.$textField.on('keyup.validate', _.bind(this.validate, this));
                    this.$textField.on('change.validate', _.bind(this.validate, this));
                }
            },

            /**
             * returns the current value from the input field
             * @extends widgets.Widget
             */
            getValue: function () {
                return this.$textField.val();
            },

            /**
             * defines the (initial) value of the input field
             * @extends widgets.Widget
             */
            setValue: function (value, triggerChange) {
                var currentValue = this.$textField.val();
                if ('' + currentValue !== '' + value) {
                    this.$textField.val(value);
                    if (triggerChange) {
                        this.$textField.trigger('change');
                    }
                }
            },

            /**
             * @extends widgets.Widget
             */
            setDefaultValue: function (value) {
                this.$textField.attr('placeholder', value);
            },

            /**
             * @param options the initializers options object
             * @returns the configuration object for the typeahead plugin
             */
            typeahead: function (options) {
                var typeahead = this.$el.data('typeahead') || options.typeahead;
                if (typeahead) {
                    if (_.isString(typeahead)) {
                        if (/^(https?:\/\/[^/]+)?\/[^/]+\/.*/i.exec(typeahead)) {
                            var url = typeahead;
                            // a typeahead service must return a JSON array of suggestions
                            // for the current text value sent as 'query' parameter
                            typeahead = function (query, callback) {
                                core.ajaxGet(url, {
                                    data: {
                                        query: query
                                    }
                                }, function (data) {
                                    callback(data);
                                })
                            };
                        } else if (typeahead.indexOf('{') === 0) {
                            typeahead = JSON.parse(typeahead);
                        } else if (typeahead.indexOf(',') > 0 && typeahead.indexOf('(') < 0) {
                            typeahead = typeahead.split(',')
                        } else {
                            try {
                                var f = eval(typeahead);
                                if (_.isFunction(f)) {
                                    typeahead = f;
                                }
                            } catch (ex) {
                            }
                        }
                    }
                    if (_.isFunction(typeahead) || _.isArray(typeahead)) {
                        return {
                            minLength: 1,
                            source: typeahead
                        };
                    }
                    return typeahead;
                }
                return undefined;
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
                return this.$input;
            },

            /**
             * resets the validation state and the input field value
             * @extends widgets.Widget
             */
            reset: function () {
                this.valid = undefined;
                this.$textField.closest('.form-group').removeClass('has-error');
                this.$textField.val(undefined);
            }
        });

        widgets.register('.widget.text-field-widget', components.TextFieldWidget);

        components.ComboBoxWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                this.$menu = this.$('.dropdown-menu');
                this.$menu.find('li a').click(_.bind(this.optionSelected, this));
                this.$textField.on('change.combobox', _.bind(this.onValueChange, this));
            },

            onValueChange: function () {
                var value = this.getValue();
                var self = this;
                this.$('[data-value-class]').each(function () {
                    var $el = $(this);
                    var css = $el.data('value-class').replace(/\$/g, value);
                    $el.removeClass().addClass(css);
                });
                this.$menu.find('li').removeClass('active');
                this.$menu.find('li[data-value="' + value + '"]').addClass('active');
            },

            optionSelected: function (event) {
                event.preventDefault();
                var $link = $(event.currentTarget);
                var value = $link.closest('li').data('value');
                this.setValue(value, true);
                this.$menu.dropdown('toggle');
                return false;
            }
        });

        widgets.register('.widget.combobox-widget', components.ComboBoxWidget);

        /**
         * the 'text-field-widget' (window.core.components.TextFieldWidget)
         *
         * this is the basic class ('superclass') of all text input field based widgets; it is also usable
         * as is for normal text input fields; it implements the general validation and reset functions
         * possible attributes:
         * - data-rules: 'required'
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
                if ('' + currentValue !== '' + value) {
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
             * resets the validation state and the input field value
             */
            reset: function () {
                this.valid = undefined;
                this.$input.closest('.form-group').removeClass('has-error');
                this.$input.val(undefined);
            }
        });

        widgets.register('.widget.text-area-widget', components.TextAreaWidget);

        /**
         * the 'abstract' PathSelector is a Widget which interacts with a PathWidget
         */
        components.PathSelector = widgets.Widget.extend({

            initialize: function (options) {
                widgets.Widget.prototype.initialize.apply(this, [options]);
                this.setPathWidget(options.pathWidget);
            },

            // getEventId: function () {...},
            // onPathChanged: function (path) {...}

            setPathWidget: function (pathWidget) {
                if (this.pathWidget) {
                    this.pathWidget.$input.off('change.' + this.getEventId());
                }
                this.pathWidget = pathWidget;
                if (this.pathWidget) {
                    this.pathWidget.$input.on('change.' + this.getEventId(), _.bind(this.pathInputChanged, this));
                }
            },

            /**
             * the callback on each change in the input field;
             * selects the node in the tree view if the nodes exists
             */
            pathInputChanged: function () {
                if (!this.busy) {
                    this.busy = true;
                    var path = this.pathWidget.getValue();
                    if (path !== this.lastPathSelected) {
                        if (path.indexOf('/') === 0) {
                            core.getJson('/bin/cpm/nodes/node.tree.json' + core.encodePath(path),
                                _.bind(function (data) {
                                    this.lastPathSelected = data.path;
                                    this.onPathChanged(data.path);
                                }, this));
                        }
                    }
                    this.busy = false;
                }
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

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [_.extend({
                    typeahead: {
                        minLength: 1,
                        source: _.bind(function (query, callback) {
                            // ensure that query is of a valid path pattern
                            if (query.indexOf('/') === 0) {
                                var rootPath = this.getRootPath();
                                if (rootPath !== '/') {
                                    query = rootPath + query;
                                }
                                core.getJson('/bin/cpm/nodes/node.typeahead.json' + query, function (data) {
                                    if (rootPath !== '/') {
                                        for (var i = 0; i < data.length; i++) {
                                            if (data[i].indexOf(rootPath + '/') === 0) {
                                                data[i] = data[i].substring(rootPath.length);
                                            }
                                        }
                                    }
                                    callback(data);
                                });
                            }
                        }, this),
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
                    }
                }, options)]);
                // retrieve element attributes
                this.dialogTitle = this.$el.attr('title');
                this.dialogLabel = this.$el.data('label');
                this.config = {
                    rootPath: this.$el.data('root') || '/'
                };
                this.setRootPath(this.config.rootPath);
                this.filter = this.$el.data('filter');
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
                selectDialog.setRootPath(this.getRootPath());
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
                var value = this.getValue();
                if (_.isFunction(callback)) {
                    callback(value);
                } else {
                    return value;
                }
            },

            /**
             * stores the value for a selected path; extension hook for different path based values
             */
            setPath: function (path) {
                var oldValue = this.getValue();
                this.setValue(path, oldValue !== path);
            },

            getRootPath: function () {
                return this.rootPath ? this.rootPath : '/';
            },

            setRootPath: function (path) {
                this.rootPath = this.adjustRootPath(path ? path : this.config.rootPath);
            },

            adjustRootPath: function (path) {
                return path;
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
                core.getJson('/bin/cpm/nodes/node.tree.json' + core.encodePath(path), _.bind(function (data) {
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
         *   - data-options: '[min][:step[:max[:default]]]'
         */
        components.NumberFieldWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                var dataOptions = this.$el.data('options');
                if (dataOptions) {
                    var values = ('' + dataOptions).split(':'); // 'toString' - split - parse...
                    if (values.length > 0) options.minValue = values[0];
                    if (values.length > 1) options.stepSize = values[1];
                    if (values.length > 2) options.maxValue = values[2];
                    if (values.length > 3) options.defValue = values[3];
                }
                this.minValue = options.minValue ? Number(options.minValue) : undefined;
                this.stepSize = Number(options.stepSize || 1);
                this.maxValue = options.maxValue ? Number(options.maxValue) : undefined;
                this.defValue = options.defValue ? Number(options.defValue) : undefined;
                this.$('.clear').click(_.bind(this.clear, this));
                this.$('.increment').click(_.bind(this.increment, this));
                this.$('.decrement').click(_.bind(this.decrement, this));
                this.initValue();
                this.$textField.on('change.number', _.bind(this.onChange, this));
            },

            initValue: function () {
                var value = this.getValue();
                if (value === undefined) {
                    value = this.defValue !== undefined ? this.defValue : this.blankAllowed() ? undefined
                        : this.minValue !== undefined ? this.minValue : this.maxValue;
                }
                this.setValue(value);
            },

            onChange: function () {
                this.setValue(this.getValue()); // filter new value for number restrictions
            },

            setValue: function (value, triggerChange) {
                if (value !== undefined && (value = this.getNumber(value)) !== undefined) {
                    if (this.minValue !== undefined && value < this.minValue) {
                        value = this.minValue;
                    }
                    if (this.maxValue !== undefined && value > this.maxValue) {
                        value = this.maxValue;
                    }
                }
                components.TextFieldWidget.prototype.setValue.apply(this, [value !== undefined ? value
                    : this.blankAllowed() ? undefined : this.defValue, triggerChange]);
            },

            blankAllowed: function () {
                return this.rules === undefined || this.rules.blank || this.rules.required !== true
            },

            getNumber: function (value) {
                if (value === undefined) {
                    value = this.getValue();
                }
                if (value !== undefined) {
                    try {
                        value = parseInt(value);
                    } catch (ex) {
                    }
                    if (isNaN(value)) {
                        value = undefined;
                    }
                }
                return value;
            },

            increment: function () {
                if (this.stepSize) {
                    var value = this.getNumber();
                    this.setValue(value !== undefined ? (value + this.stepSize) : this.minValue, true);
                }
            },

            decrement: function () {
                if (this.stepSize) {
                    var value = this.getNumber();
                    this.setValue(value !== undefined ? (value - this.stepSize) : this.maxValue, true);
                }
            },

            clear: function () {
                this.setValue(undefined, true);
            },

            extValidate: function (value) {
                value = this.getNumber(value);
                var valid = value !== undefined || this.blankAllowed();
                if (valid && value !== undefined) {
                    if (this.minValue !== undefined) {
                        valid = value >= this.minValue;
                    }
                    if (valid && this.maxValue !== undefined) {
                        valid = value <= this.maxValue;
                    }
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
                this.data = {
                    locale: this.$el.data('locale') || 'en',
                    format: this.$el.data('format') || 'YYYY-MM-DD HH:mm:ss',
                    options: {
                        weeks: core.toBoolean(this.$el.data('weeks'), true)
                    }
                };
                this.$el.datetimepicker({
                    locale: this.data.locale,
                    format: this.data.format,
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
                        'DD.MM.YYYY HH:mm:ss ZZ',
                        'D. MMMM YYYY',
                        'D. MMMM YYYY HH:mm',
                        'D. MMMM YYYY HH:mm ZZ',
                        'D MMM YYYY',
                        'D MMM YYYY HHmm',
                        'D MMM YYYY HHmm ZZ',
                        'D MMM YYYY HH:mm',
                        'D MMM YYYY HH:mm ZZ',
                        'MMMM D, YYYY',
                        'MMMM D, YYYY HHmm',
                        'MMMM D, YYYY HHmm ZZ',
                        'MMMM D, YYYY HH:mm',
                        'MMMM D, YYYY HH:mm ZZ',
                        'MM/DD/YYYY',
                        'MM/DD/YYYY HHmm',
                        'MM/DD/YYYY HHmm ZZ',
                        'MM/DD/YYYY HH:mm',
                        'MM/DD/YYYY HH:mm ZZ'
                    ],
                    calendarWeeks: this.data.options.weeks,
                    showTodayButton: true,
                    showClear: true,
                    showClose: true
                });
                this.datetimepicker = this.$el.data('DateTimePicker');
            },

            /**
             * defines the (initial) value of the input field
             */
            setValue: function (value, triggerChange) {
                this.datetimepicker.date(value ? moment(value, this.data.format) : null);
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
         *  'browse:<Label>(:<Title>)', 'remove:<Label>(:<Title>)','upload:<Label>(:<Title>)',
         */
        components.FileUploadWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [options]);
                var dataOptions = this.$el.data('options');
                options.showPreview = true;
                options.showUpload = false;
                options.fileType = 'any';
                if (dataOptions) {
                    var idx;
                    if (dataOptions.indexOf('hidePreview') >= 0) {
                        options.showPreview = false;
                        options.showUploadedThumbs = false;
                    }
                    if (dataOptions.indexOf('showUpload') >= 0) {
                        options.showUpload = true;
                    }
                    this.getOptionText(options, dataOptions, 'browse');
                    this.getOptionText(options, dataOptions, 'remove');
                    this.getOptionText(options, dataOptions, 'upload');
                }
                var dataType = this.$el.data('type');
                if (dataType) {
                    options.fileType = dataType;
                }
                this.whatever = this.$textField.fileinput(options);
                this.$widget = this.$el.closest('.file-input-new');
                this.$inputCaption = this.$widget.find('.kv-fileinput-caption');
            },

            getOptionText: function (target, dataOptions, key) {
                var idx;
                if ((idx = dataOptions.indexOf(key + ':')) >= 0) {
                    target[key + 'Label'] = dataOptions.substring(idx + key.length + 1).trim();
                    if ((idx = target[key + 'Label'].indexOf(',')) > 0) {
                        target[key + 'Label'] = target[key + 'Label'].substring(0, idx).trim();
                    }
                    if ((idx = target[key + 'Label'].indexOf(':')) > 0) {
                        target[key + 'Title'] = target[key + 'Label'].substring(idx + 1).trim();
                        target[key + 'Label'] = target[key + 'Label'].substring(0, idx).trim();
                    }
                }
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
            },

            setName: function (name) {
                this.$textField.attr('name', name);
            },

            getFileName: function () {
                var files = this.$textField.fileinput('getFileStack');
                return files.length === 1 ? files[0].name : undefined;
            }
        });

        widgets.register('.widget.file-upload-widget', components.FileUploadWidget);

        /**
         * the 'property-name-widget' (window.core.components.RepositoryNameWidget)
         */
        components.PropertyNameWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [_.extend({
                    // let browsers autocomplete as is and add typeahead function to the input field
                    typeahead: {
                        autocomplete: 'auto',
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
                    }
                }, options)]);
            }
        });

        widgets.register('.widget.property-name-widget', components.PropertyNameWidget);

        /**
         * the 'repository-name-widget' (window.core.components.RepositoryNameWidget)
         */
        components.RepositoryNameWidget = components.TextFieldWidget.extend({

            nameChanged: function (name, contextWidget) {
                // set 'subtypes' of name for useful typeahead
                if ('jcr:primaryType' === name) {
                    contextWidget.setWidgetType.apply(contextWidget, ['jcr-primaryType']);
                } else if ('jcr:mixinTypes' === name) {
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
                components.TextFieldWidget.prototype.initialize.apply(this, [_.extend({
                    typeahead: '/bin/cpm/core/system.primaryTypes.json'
                }, options)]);
            }
        });

        widgets.register('.widget.primary-type-widget', components.PrimaryTypeWidget);

        /**
         * the 'mixin-type-widget' (window.core.components.MixinTypeWidget)
         */
        components.MixinTypeWidget = components.TextFieldWidget.extend({

            initialize: function (options) {
                components.TextFieldWidget.prototype.initialize.apply(this, [_.extend({
                    typeahead: '/bin/cpm/core/system.mixinTypes.json'
                }, options)]);
            }
        });

        widgets.register('.widget.mixin-type-widget', components.MixinTypeWidget);

    })(core.components, window.widgets);

})(window.core);
