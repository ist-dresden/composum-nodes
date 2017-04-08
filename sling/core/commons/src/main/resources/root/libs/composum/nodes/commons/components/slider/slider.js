(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components) {

        components.const = _.extend(components.const || {}, {
            slider: {
                css: {
                    base: 'slider-widget'
                }
            }
        });

        components.SliderWidget = window.widgets.Widget.extend({

            initialize: function (options) {
                window.widgets.Widget.prototype.initialize.apply(this, [options]);
                this.$input.attr('data-slider-value', this.$input.val());
                this.$input.slider();
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
            }
        });

        window.widgets.register(window.widgets.const.css.selector.prefix +
            components.const.slider.css.base, components.SliderWidget, {

            /**
             * reset a cloned instance to the 'original' DOM element only
             */
            afterClone: function () {
                $(this).find('.slider').remove();
            }
        });

    })(core.components);

})(window.core);
