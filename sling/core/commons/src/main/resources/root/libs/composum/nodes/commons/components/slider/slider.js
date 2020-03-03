(function () {
    'use strict';
    CPM.namespace('core.components');

    (function (components, core) {

        components.const = _.extend(components.const || {}, {
            slider: {
                css: {
                    base: 'slider-widget'
                }
            }
        });

        components.SliderWidget = CPM.widgets.Widget.extend({

            initialize: function (options) {
                CPM.widgets.Widget.prototype.initialize.apply(this, [options]);
                this.$input.attr('data-slider-value', this.$input.val());
                this.$input.slider();
            },

            getValue: function () {
                return this.$input.val();
            },

            setValue: function (value, triggerChange) {
                this.$input.val(value);
                if (triggerChange) {
                    this.$el.trigger('change', [value]);
                }
            },

            reset: function () {
                this.$input.val(undefined);
            }
        });

        CPM.widgets.register(CPM.widgets.const.css.selector.prefix +
            components.const.slider.css.base, components.SliderWidget, {

            /**
             * reset a cloned instance to the 'original' DOM element only
             */
            afterClone: function () {
                $(this).find('.slider').remove();
            }
        });

    })(CPM.core.components, CPM.core);

})();
