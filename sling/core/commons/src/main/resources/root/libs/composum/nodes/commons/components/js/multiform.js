/**
 *
 *
 */
(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components, widgets) {

        //
        // Multi 'Form'
        //

        components.const = _.extend(components.const || {}, {
            multiform: {
                css: {
                    base: 'multi-form-widget',
                    current: 'current',
                    selector: {
                        item: '.multi-form-item',
                        handle: '.item-select',
                        widget: '.widget',
                        richtext: '.richtext-widget',
                        actions: '.action-bar',
                        table: '_table'
                    }
                },
                data: {
                    name: 'name'
                }
            }
        });

        /**
         * a generic item implementation for a MultiFormWidget item
         */
        components.MultiFormItem = Backbone.View.extend({

            initialize: function (options) {
            },

            /**
             * returns the current set of values of the item
             */
            getValue: function () {
                var c = components.const.multiform;
                var value = {};
                this.$(c.css.selector.widget).each(function () {
                    if (this.view) {
                        if (_.isFunction(this.view.getValue)) {
                            var name = core.getWidgetName(this.view);
                            if (name) {
                                value[name] = this.view.getValue.apply(this.view);
                            }
                        }
                    }
                });
                var values = _.values(value);
                if (values.length == 1) {
                    value = values[0];
                }
                return value;
            },

            /**
             * presets the values of the item
             */
            setValue: function (value) {
                var c = components.const.multiform;
                this.$(c.css.selector.widget).each(function () {
                    if (this.view) {
                        if (_.isFunction(this.view.setValue)) {
                            var name = core.getWidgetName(this.view);
                            this.view.setValue.apply(this.view, [_.isObject(value) ? value[name] : value]);
                        }
                    }
                });
            },

            reset: function () {
                var c = components.const.multiform;
                var $widgets = this.$(c.css.selector.widget);
                if ($widgets.length > 0) {
                    $widgets.each(function () {
                        if (this.view && _.isFunction(this.view.reset)) {
                            this.view.reset.apply(this.view);
                        }
                    });
                } else {
                    this.$('input').val(undefined);
                }
            }
        });

        /**
         * the generic multi form widget manages a set of structured form items
         */
        components.MultiFormWidget = Backbone.View.extend({

            initialize: function (options) {
                var c = components.const.multiform;
                this.itemList = [];
                this.itemType = options.itemType || components.MultiFormItem;
                this.name = this.$el.data(c.data.name) || 'name';
                var $itemElements = this.$(c.css.selector.item);
                for (var i = 0; i < $itemElements.length; i++) {
                    this.itemList[i] = this.newItem($itemElements[i]);
                }
                this.current(this.itemList[0]);
                if (this.$(c.css.selector.actions).length == 0) {
                    this.$el.append(
                        '<div class="action-bar btn-toolbar" role="toolbar">' +
                        '<div class="btn-group btn-group-sm">' +
                        '<button type="button" class="add glyphicon-plus glyphicon btn btn-default" title="Add Filter"><span class="label">Add</span></button>' +
                        '<button type="button" class="remove glyphicon-minus glyphicon btn btn-default" title="Remove Filter"><span class="label">Remove</span></button>' +
                        '</div>' +
                        '<div class="btn-group btn-group-sm">' +
                        '<button type="button" class="move-up glyphicon-chevron-up glyphicon btn btn-default" title="Move Up"><span class="label">Up</span></button>' +
                        '<button type="button" class="move-down glyphicon-chevron-down glyphicon btn btn-default" title="Move Down"><span class="label">Down</span></button>' +
                        '</div>' +
                        '</div>');
                }
                this.$(c.css.selector.actions + ' button.add').unbind('click').on('click', _.bind(this.add, this));
                this.$(c.css.selector.actions + ' button.remove').unbind('click').on('click', _.bind(this.remove, this));
                this.$(c.css.selector.actions + ' button.move-up').unbind('click').on('click', _.bind(this.moveUp, this));
                this.$(c.css.selector.actions + ' button.move-down').unbind('click').on('click', _.bind(this.moveDown, this));
            },

            getValue: function () {
                var valueSet = [];
                for (var i = 0; i < this.itemList.length; i++) {
                    var value = this.itemList[i].getValue();
                    if (i < this.itemList.length - 1 || !core.isEmptyObject(value)) {
                        valueSet.push(value);
                    }
                }
                return valueSet;
            },

            setValue: function (valueSet) {
                if (valueSet) {
                    this.reset(valueSet.length);
                    for (var i = 0; i < this.itemList.length; i++) {
                        this.itemList[i].setValue(valueSet[i]);
                    }
                } else {
                    this.reset(0);
                }
            },

            reset: function (size) {
                if (!size || size < 1) {
                    size = 1;
                }
                while (this.itemList.length > size) {
                    this.remove();
                }
                for (var i = 0; i < this.itemList.length; i++) {
                    this.itemList[i].reset();
                }
                while (this.itemList.length < size) {
                    this.add();
                }
            },

            newItem: function (element) {
                var c = components.const.multiform;
                var item = core.getView(element, this.itemType, _.bind(function (item) {
                    if (item.$(c.css.selector.handle).length == 0) {
                        item.$el.prepend('<input class="item-select form-control" type="radio" name="item-select-' + this.name + '">');
                    }
                    item.$(c.css.selector.handle).unbind('click').on('click', _.bind(this.onSelect, this));
                    window.widgets.setUp(item.el);
                }, this), this.itemType != components.MultiFormItem);
                return item;
            },

            onSelect: function (event) {
                var c = components.const.multiform;
                var itemElement = $(event.currentTarget).closest('.multi-form-item')[0];
                for (var i = 0; i < this.itemList.length; i++) {
                    if (itemElement === this.itemList[i].el) {
                        this.itemList[i].$(c.css.selector.handle).unbind('click');
                        this.current(this.itemList[i]);
                        this.itemList[i].$(c.css.selector.handle).on('click', _.bind(this.onSelect, this));
                        break;
                    }
                }
            },

            indexOf: function (item) {
                return _.indexOf(this.itemList, item);
            },

            current: function (item) {
                var c = components.const.multiform;
                if (item && this.indexOf(item) >= 0) {
                    this.currentItem = item;
                    for (var i = 0; i < this.itemList.length; i++) {
                        this.itemList[i].$el.removeClass(c.css.current);
                        this.itemList[i].$(c.css.selector.handle).attr('value', i);
                    }
                    this.currentItem.$el.addClass(c.css.current);
                    this.currentItem.$(c.css.selector.handle).click();
                }
                return this.currentItem;
            },

            add: function () {
                var currentState = this.currentItem;
                var template = this.itemList[this.itemList.length - 1];
                var $clone = (_.isFunction(template.clone) ? template.clone() : template.$el.clone());
                template.$el.parent().append($clone);
                $clone.each(function () {
                    widgets.apply(this, 'afterClone');
                });
                var newItem = this.newItem($clone);
                newItem.reset();
                this.itemList[this.itemList.length] = newItem;
                this.current(currentState);
            },

            remove: function () {
                if (this.itemList.length > 1) {
                    var currentIndex = this.indexOf(this.currentItem);
                    if (currentIndex == this.itemList.length - 1) {
                        currentIndex--;
                    }
                    this.currentItem.$el.remove();
                    this.itemList = _.without(this.itemList, this.currentItem);
                    this.current(this.itemList[currentIndex]);
                } else {
                    this.itemList[0].reset();
                }
            },

            moveUp: function () {
                var index = this.indexOf(this.currentItem);
                if (index > 0) {
                    var head = index > 1 ? _.first(this.itemList, index - 1) : [];
                    var tail = index < this.itemList.length - 1 ? _.rest(this.itemList, index + 1) : [];
                    this.itemList = _.union(head, [this.currentItem], [this.itemList[index - 1]], tail);
                    this.currentItem.$el.insertBefore(this.currentItem.$el.prev());
                }
            },

            moveDown: function () {
                var index = this.indexOf(this.currentItem);
                if (index < this.itemList.length - 1) {
                    var head = index > 0 ? _.first(this.itemList, index) : [];
                    var tail = index < this.itemList.length - 2 ? _.rest(this.itemList, index + 2) : [];
                    this.itemList = _.union(head, [this.itemList[index + 1]], [this.currentItem], tail);
                    this.currentItem.$el.insertAfter(this.currentItem.$el.next());
                }
            },

            getWidgetValue: function (element) {
                var widget = core.widgetOf(element, this);
                if (widget && _.isFunction(widget.getValue)) {
                    return widget.getValue();
                } else {
                    return element.val();
                }
            },

            setWidgetValue: function (element, value) {
                var widget = core.widgetOf(element, this);
                if (widget && _.isFunction(widget.setValue)) {
                    widget.setValue(value);
                } else {
                    element.val(value);
                }
            }
        });

        widgets.register(widgets.const.css.selector.prefix +
            components.const.multiform.css.base, components.MultiFormWidget);

    })(core.components, window.widgets);

})(window.core);
