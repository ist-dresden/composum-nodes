/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.browser');

    (function (browser, core) {

        browser.getPropertyDialog = function (path) {
            return core.getView('#browser-view-property-dialog', browser.PropertyDialog, {
                path: path
            });
        };

        browser.openNewPropertyDialog = function (callback, path) {
            var dialog = browser.getPropertyDialog(path);
            dialog.show(_.bind(function () {
                dialog.setProperty(new browser.Property({
                        path: path
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
                core.ajaxPut("/bin/cpm/nodes/property.json" + core.encodePath(this.get('path')), JSON.stringify(data), {
                    dataType: 'json'
                }, onSuccess, onError);
            },

            destroy: function (onSuccess, onError) {
                core.ajaxDelete("/bin/cpm/nodes/property.remove.json" + core.encodePath(this.get('path')), {
                    data: JSON.stringify({
                        "names": [this.get('name')]
                    }),
                    dataType: 'json'
                }, onSuccess, onError);
            }
        });

        browser.PropertyDialog = core.components.Dialog.extend({

            initialize: function (options) {
                this.path = options.path;
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$title = this.$('h4.modal-title');
                this.$path = this.$('input[name="path"]');
                this.$oldname = this.$('input[name="oldname"]');
                this.$name = this.$('input[name="name"]');
                this.$type = this.$('select[name="type"]');
                this.$subtype = this.$('.subtype-select');
                this.subtype = core.getWidget(this.$subtype, '.widget.select-widget', core.components.SelectWidget);
                this.$multi = this.$('input[name="multi"]');
                this.valueWidget = core.getWidget(this.$el,
                    '.widget.property-value-widget', browser.PropertyValueWidget);

                this.loadTypes();
                this.$type.on('change', _.bind(this.typeChanged, this));
                this.subtype.changed('PropertyDialog', _.bind(this.subtypeChanged, this));
                this.subtype.$input.focus(_.bind(function () {
                    this.subtype.lastValue = this.subtype.getValue();
                }, this));
                this.$multi.on('change', _.bind(this.multiChanged, this));
                this.$name.on('change', _.bind(this.nameChanged, this));

                this.$delete = this.$('button.delete');
                this.$save = this.$('button.save');
                this.$upload = this.$('button.upload');
                this.$delete.click(_.bind(this.deleteProperty, this));
                this.$save.click(_.bind(this.saveProperty, this));
                this.$upload.click(_.bind(this.uploadBinary, this));
            },

            readonly: function () {
                this.$delete.attr('disabled', 'disabled');
                this.$save.attr('disabled', 'disabled');
                this.$upload.attr('disabled', 'disabled');
            },

            typeChanged: function () {
                var currentValue = this.valueWidget.getValue();
                var type = this.$type.val();
                this.$subtype.css('visibility', type === 'String' ? 'visible' : 'hidden');
                if (!this.busy) {
                    this.busy = true;
                    var subtype = (type === 'String' ? this.subtype.getValue() : undefined);
                    this.valueWidget.setType(type, subtype);
                    this.busy = false;
                }
                if (type === 'Binary') {
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

            subtypeChanged: function (event, value) {
                if (value === 'richtext') {
                    core.getJson('/bin/cpm/nodes/property.xss.json?value=' + encodeURIComponent(this.valueWidget.getValue()),
                        _.bind(function (data) {
                            if (data.warning) {
                                core.i18n.get('The value contains XSS stuff. A switch could trigger its execution. Do you really want to switch?', _.bind(function (text) {
                                    if (confirm(text)) {
                                        this.typeChanged();
                                    } else {
                                        this.subtype.setValue(this.subtype.lastValue);
                                    }
                                }, this));
                            } else {
                                this.typeChanged();
                            }
                        }, this))
                } else {
                    this.typeChanged();
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
                var subtype = property.get('subtype') || 'string';
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
                    this.$name.val(undefined);
                    this.$oldname.val(undefined);
                    this.$title.empty().html('Create Property');
                    this.$delete.addClass('hidden');
                }
                this.nameChanged();
                this.valueWidget.setValue(value);
            },

            getProperty: function () {
                return new browser.Property({
                    path: this.$path.val(),
                    name: this.$name.val(),
                    oldname: this.$oldname.val(),
                    type: this.$type.val(),
                    multi: this.$multi.prop('checked'),
                    value: this.valueWidget.getValue()
                });
            },

            uploadBinary: function () {
                this.submitForm(_.bind(function () {
                    this.hide();
                }, this));
            },

            saveProperty: function () {
                if (this.form.isValid()) {
                    this.lock();
                    this.getProperty().save(_.bind(function () {
                            $(document).trigger('path:changed', [this.path]);
                            this.hide();
                        }, this),
                        _.bind(function (result) {
                            this.unlock();
                            this.alert('danger', 'error on save property', result);
                        }, this)
                    );
                } else {
                    this.alert('danger', 'name, type and value must be specified');
                }
            },

            deleteProperty: function () {
                this.lock();
                this.getProperty().destroy(_.bind(function () {
                        $(document).trigger('path:changed', [this.path]);
                        this.hide();
                    }, this),
                    _.bind(function (result) {
                        this.unlock();
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
                if (!this.type || this.type !== type || this.subtype !== subtype) {
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
                if (!this.typeSelector || this.typeSelector !== typeSelector) {
                    this.typeSelector = typeSelector;
                    this.$('.widget').each(function () {
                        var $widget = $(this);
                        var widget = $widget[0].view;
                        if (widget) {
                            $widget.addClass('hidden');
                            widget.$input.removeAttr('name');
                            widget.$el.removeAttr('data-rules');
                            if (_.isFunction(widget.reset)) {
                                widget.reset.apply(widget);
                            }
                        }
                    });
                    var propertyWidget = this;
                    var propertyName = propertyWidget.$el.attr('data-name');
                    var propertyRules = propertyWidget.$el.attr('data-rules');
                    this.$('.widget.property-type-' + typeSelector).each(function () {
                        var $widget = $(this);
                        var widget = $widget[0].view;
                        if (widget) {
                            $widget.removeClass('hidden');
                            widget.$input.attr('name', propertyName || 'value');
                            if (propertyRules) {
                                widget.$el.attr('data-rules', propertyRules);
                            }
                        }
                    });
                }
            },

            setMultiValue: function (multi) {
                if (!this.multiValue || this.multiValue !== multi) {
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

            setValue: function (value, triggerChange) {
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
                if (triggerChange) {
                    this.$el.trigger('change', [value]);
                }
            }
        });

    })(CPM.nodes.browser, CPM.core);

})();
