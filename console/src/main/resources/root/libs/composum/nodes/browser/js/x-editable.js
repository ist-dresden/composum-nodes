(function ($) {
    "use strict";

    var Checkbox = function (options) {
        this.init('checkbox', options, Checkbox.defaults);
    };

    $.fn.editableutils.inherit(Checkbox, $.fn.editabletypes.abstractinput);

    $.extend(Checkbox.prototype, {
        render: function () {

            this.$tpl.empty();
            $('<div>').append($('<input>', {
                type: 'checkbox'
            })).appendTo(this.$tpl);

            this.$input = this.$tpl.find('input[type="checkbox"]');
            this.setClass();
        },

        value2str: function (value) {
            return value ? 'true' : 'false';
        },

        str2value: function (str) {
            var value = false;
            if (typeof str === 'string' && str.length) {
                value = (str.toLowerCase() == 'true')
            }
            return value;
        },

        value2input: function (value) {
            this.$input.prop('checked', value !== undefined && value);
        },

        input2value: function () {
            var checked = this.$input.prop('checked');
            return checked !== undefined && checked;
        },

        activate: function () {
            this.$input.focus();
        },

        autosubmit: function () {
            this.$input.on('keydown', function (e) {
                if (e.which === 13) {
                    $(this).closest('form').submit();
                }
            });
        }
    });

    Checkbox.defaults = $.extend({}, $.fn.editabletypes.abstractinput.defaults, {
        /**
         @property tpl
         @default <div></div>
         **/
        tpl: '<div class="editable-checkbox"></div>',

        /**
         @property inputclass
         @type string
         @default null
         **/
        inputclass: null
    });

    $.fn.editabletypes.checkbox = Checkbox;

}(window.jQuery));
