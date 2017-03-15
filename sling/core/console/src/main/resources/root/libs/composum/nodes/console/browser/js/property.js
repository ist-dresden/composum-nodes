/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {

        browser.getPropertyDialog = function () {
            return core.getView('#browser-view-property-dialog', window.core.browser.PropertyDialog);
        };

        browser.openNewPropertyDialog = function (callback) {
            var dialog = browser.getPropertyDialog();
            dialog.show(_.bind(function () {
                dialog.setProperty(new browser.Property({
                        path: browser.getCurrentPath()
                    })
                );
            }, this), callback);
        };

        browser.Property = Backbone.Model.extend({

            defaults: {
                path: undefined,
                name: undefined,
                oldname: undefined,
                type: 'String',
                multi: false,
                value: undefined
            },

            validate: function () {
                var path = this.get('path');
                var name = this.get('name');
                var type = this.get('type');
                return (path && path.trim().length > 0 &&
                name && name.trim().length > 0 &&
                type && type.trim().length > 0);
            },

            save: function (onSuccess, onError, data) {
                if (!data) {
                    data = this.toJSON();
                }
                core.ajaxPut("/bin/cpm/nodes/property.json" + this.get('path'), JSON.stringify(data), {
                    dataType: 'json'
                }, onSuccess, onError);
            },

            destroy: function (onSuccess, onError) {
                core.ajaxDelete("/bin/cpm/nodes/property.remove.json" + this.get('path'), {
                    data: JSON.stringify({
                        "names": [this.get('name')]
                    }),
                    dataType: 'json'
                }, onSuccess, onError);
            }
        });

        browser.PropertyDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$title = this.$('h4.modal-title');
                this.$path = this.$('input[name="path"]');
                this.$oldname = this.$('input[name="oldname"]');
                this.$name = this.$('input[name="name"]');
                this.$type = this.$('select[name="type"]');
                this.$subtype = this.$('.subtype');
                this.subtype = core.getWidget(this.$subtype, '.widget.select-widget', core.components.SelectWidget);
                this.$multi = this.$('input[name="multi"]');
                this.valueWidget = core.getWidget(this.$el,
                    '.widget.property-value-widget', browser.PropertyValueWidget);

                this.loadTypes();
                this.$type.on('change', _.bind(this.typeChanged, this));
                this.subtype.$el.on('change', _.bind(this.typeChanged, this));
                this.$multi.on('change', _.bind(this.multiChanged, this));
                this.$name.on('change', _.bind(this.nameChanged, this));

                this.$delete = this.$('button.delete');
                this.$delete.click(_.bind(this.deleteProperty, this));
                this.$('button.save').click(_.bind(this.saveProperty, this));
                this.$('button.upload').click(_.bind(this.uploadBinary, this));
            },

            typeChanged: function () {
                var currentValue = this.valueWidget.getValue();
                var type = this.$type.val();
                this.$subtype.css('visibility', type == 'String' ? 'visible' : 'hidden');
                if (!this.busy) {
                    this.busy = true;
                    var subtype = (type == 'String' ? this.subtype.getValue() : undefined);
                    this.valueWidget.setType(type, subtype);
                    this.busy = false;
                }
                if (type == 'Binary') {
                    this.form.$el.removeClass('default');
                    this.form.$el.addClass('binary');
                    if (this.$multi.prop('checked')) {
                        this.$multi.prop('checked', false);
                        this.multiChanged();
                    }
                    this.$multi.closest('.form-group').addClass('invisible');
                } else {
                    this.form.$el.removeClass('binary');
                    this.form.$el.addClass('default');
                    this.$multi.closest('.form-group').removeClass('invisible');
                }
                if (currentValue) {
                    this.valueWidget.setValue(currentValue);
                }
            },

            multiChanged: function () {
                if (!this.busy) {
                    this.busy = true;
                    var multi = this.$multi.prop('checked');
                    this.valueWidget.setMultiValue(multi);
                    this.busy = false;
                }
            },

            nameChanged: function () {
                if (!this.busy) {
                    this.busy = true;
                    var name = this.$name.val();
                    this.valueWidget.nameChanged(name);
                    this.busy = false;
                }
            },

            setProperty: function (property) {
                this.reset();
                var path = property.get('path');
                var type = property.get('type');
                var name = property.get('name');
                var value = undefined;
                if (name) {
                    value = property.get('value');
                }
                var subtype = 'string';
                if (type === 'String') {
                    if (value) {
                        if (value.indexOf('\n') >= 0) {
                            subtype = 'plaintext';
                        }
                        if (value.indexOf('</') >= 0) {
                            subtype = 'richtext';
                        }
                    }
                }
                this.$path.val(path);
                this.subtype.setValue(subtype);
                if (name) {
                    this.$type.val(type);
                    this.typeChanged();
                    var multi = property.get('multi');
                    this.$multi.prop('checked', multi);
                    this.multiChanged();
                    this.$name.val(name);
                    this.$oldname.val(name);
                    this.$title.empty().html('Change Property');
                    this.$delete.removeClass('hidden');
                } else {
                    this.$type.val('String');
                    this.typeChanged();
                    this.$multi.prop('checked', false);
                    this.multiChanged();
                    this.$title.empty().html('Create Property');
                    this.$delete.addClass('hidden');
                }
                this.nameChanged();
                this.valueWidget.setValue(value);
            },

            getProperty: function () {
                var property = new browser.Property({
                    path: this.$path.val(),
                    name: this.$name.val(),
                    oldname: this.$oldname.val(),
                    type: this.$type.val(),
                    multi: this.$multi.prop('checked'),
                    value: this.valueWidget.getValue()
                });
                return property;
            },

            uploadBinary: function () {
                this.submitForm(_.bind(function () {
                    this.hide();
                }, this));
            },

            saveProperty: function () {
                if (this.form.isValid()) {
                    this.getProperty().save(_.bind(function () {
                            $(document).trigger('path:changed', [browser.getCurrentPath()]);
                            this.hide();
                        }, this),
                        _.bind(function (result) {
                            this.alert('danger', 'error on save property', result);
                        }, this)
                    );
                } else {
                    this.alert('danger', 'name, type and value must be specified');
                }
            },

            deleteProperty: function () {
                this.getProperty().destroy(_.bind(function () {
                        $(document).trigger('path:changed', [browser.getCurrentPath()]);
                        this.hide();
                    }, this),
                    _.bind(function (result) {
                        this.alert('danger', 'error on delete property', result);
                    }, this)
                );
            },

            loadTypes: function () {
                core.getJson("/bin/cpm/core/system.propertyTypes.json", _.bind(function (propertyTypes) {
                    for (var i = 0; i < propertyTypes.length; i++) {
                        this.$type.append('<option value="' + propertyTypes[i] + '">' + propertyTypes[i] + '</option>');
                    }
                }, this));
            }
        });

        /**
         * the generic 'property-value-widget' derived from the 'multi-value-widget'
         */
        browser.PropertyValueWidget = core.components.MultiFormWidget.extend({

            typeMap: {
                'String': {
                    selector: 'default',
                    subtype: {
                        'string': 'default',
                        'plaintext': 'plaintext',
                        'richtext': 'richtext'
                    }
                },
                'Name': {selector: 'name'},
                'URI': {selector: 'default'},
                'Boolean': {selector: 'boolean'},
                'Long': {selector: 'number'},
                'Date': {selector: 'datetime'},
                'Binary': {selector: 'binary'},
                'Decimal': {selector: 'number'},
                'Double': {selector: 'number'},
                'Path': {selector: 'path'},
                'Reference': {selector: 'reference'},
                'WeakReference': {selector: 'reference'}
            },

            initialize: function (options) {
                core.components.MultiFormWidget.prototype.initialize.apply(this, [options]);
                this.setMultiValue(options.multiValue);
                this.setType(options.type);
            },

            /**
             * Delegates changes of the property name to the value widgets to provide special 'subtypes'.
             */
            nameChanged: function (name) {
                var propertyWidget = this;
                this.$('.widget').each(function () {
                    if (this.view && _.isFunction(this.view.nameChanged)) {
                        this.view.nameChanged.apply(this.view, [name, propertyWidget]);
                    }
                });
            },

            /**
             * Set / Change the JCR property type - reset to the current type if no new type is given here
             * (this is useful to reset 'subtypes' (widget types) to the default type).
             */
            setType: function (type, subtype) {
                if (!this.type || this.type != type || this.subtype != subtype) {
                    // use new type or let it unchanged or fallback to 'String'
                    this.type = type || this.type || 'String';
                    this.subtype = subtype;
                    var typeRule = this.typeMap[this.type];
                    var typeSelector = undefined;
                    if (subtype && typeRule.subtype) {
                        typeSelector = typeRule.subtype[subtype];
                    }
                    if (!typeSelector) {
                        typeSelector = typeRule.selector;
                    }
                    this.setWidgetType(typeSelector);
                }
            },

            /**
             * Set the appropriate widget type for a JCR property type or set a special widget type as 'subtype'
             * of the current JCR property type (the last case is used directly by the PropertyNameWidget).
             */
            setWidgetType: function (typeSelector) {
                if (!this.typeSelector || this.typeSelector != typeSelector) {
                    this.typeSelector = typeSelector;
                    this.$('.widget').each(function () {
                        var $widget = $(this);
                        $widget.addClass('hidden');
                        var $input = $widget.is('input,textarea') ? $widget : $widget.find('input,textarea');
                        $input.removeAttr('name');
                        $input.removeAttr('data-rules');
                        if (_.isFunction($widget.reset)) {
                            $widget.reset.apply($widget[0]);
                        }
                    });
                    var propertyWidget = this;
                    this.$('.widget.' + typeSelector).each(function () {
                        var $widget = $(this);
                        $widget.removeClass('hidden');
                        var $input = $widget.is('input,textarea') ? $widget : $widget.find('input,textarea');
                        $input.attr('name', propertyWidget.$el.attr('data-name') || 'value');
                        var rules = propertyWidget.$el.attr('data-rules');
                        if (rules) {
                            $input.attr('data-rules', rules);
                        }
                    });
                }
            },

            setMultiValue: function (multi) {
                if (!this.multiValue || this.multiValue != multi) {
                    this.multiValue = multi || false;
                    if (this.multiValue) {
                        this.$el.removeClass('single-value');
                        this.$('.action-bar').removeClass('hidden');
                    } else {
                        this.reset();
                        this.$('.action-bar').addClass('hidden');
                        this.$el.addClass('single-value');
                    }
                }
            },

            getValue: function () {
                var value = this.multiValue ? [] : this.getWidgetValue('[name="value"]');
                if (this.multiValue) {
                    value = core.components.MultiFormWidget.prototype.getValue.apply(this);
                }
                return value;
            },

            setValue: function (value) {
                if (this.multiValue && _.isArray(value)) {
                    this.reset(value.length);
                    var values = this.$('[name="value"]');
                    for (var i = 0; i < values.length; i++) {
                        this.setWidgetValue($(values[i]), value[i]);
                    }
                } else {
                    this.reset();
                    this.setWidgetValue('[name="value"]', value);
                }
            }
        });

    })(core.browser);

})(window.core);
