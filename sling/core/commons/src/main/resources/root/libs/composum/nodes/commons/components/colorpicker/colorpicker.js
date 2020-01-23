(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components) {

        components.const = _.extend(components.const || {}, {
            colorpicker: {
                css: {
                    base: 'colorpicker-widget'
                }
            }
        });

        components.ColorpickerWidget = window.widgets.Widget.extend({

            initialize: function (options) {
                window.widgets.Widget.prototype.initialize.apply(this, [options]);
                // scan 'rules' attribute
                this.initRules(this.$el);
                options = _.extend({
                    format: 'hex',
                    input: this.$input
                }, options);
                this.$el.colorpicker(options);
            },

            /**
             * @extends widgets.Widget
             */
            getValue: function () {
                return this.$el.colorpicker('getValue');
            },

            /**
             * @extends widgets.Widget
             */
            setValue: function (value, triggerChange) {
                this.$el.colorpicker('setValue', value);
                if (!value) {
                    this.$input.val(undefined);
                }
                if (triggerChange) {
                    this.$el.trigger('change', [value]);
                }
            },

            /**
             * @extends widgets.Widget
             */
            setDefaultValue: function (value) {
                this.$input.attr('placeholder', value);
                var val = this.$input.val();
                this.setValue(value);
                this.$input.val(val);
            },

            /**
             * @extends widgets.Widget
             */
            reset: function () {
                this.setValue(undefined);
            }
        });

        window.widgets.register(window.widgets.const.css.selector.prefix +
            components.const.colorpicker.css.base, components.ColorpickerWidget);

    })(core.components);

})(window.core);
