(function (setup, core) {
    'use strict';

    setup.const = _.extend(setup.const || {}, {
        css: {
            base: 'composum-nodes-security-config',
            _: {
                category: '_category',
                form: '_scripts',
                scripts: '_scripts-wrapper',
                current: '_current-script',
                all: '_actions-all-checkbox',
                out: '_bottom'
            }
        }
    });

    setup.View = Backbone.View.extend({

        initialize: function (options) {
            var c = setup.const.css;
            this.$category = this.$('.' + c.base + c._.category);
            this.$out = this.$('.' + c.base + c._.out);
            this.$form = this.$('.' + c.base + c._.form);
            this.$scripts = this.$form.find('.' + c.base + c._.scripts);
            this.$current = this.$form.find('.' + c.base + c._.current);
            this.$all = this.$('.' + c.base + c._.all);
            this.$category.find('[name="category"]').on('change', _.bind(this.reload, this));
            this.$form.on('submit', _.bind(this.run, this));
            this.$all.on('change', _.bind(this.toggleAll, this));
            core.ajaxGet(this.$current.data('path'), {
                dataType: 'text'
            }, _.bind(function (content) {
                this.$current.text(content);
            }, this));
        },

        toggleAll: function () {
            var checked = this.$all.prop('checked');
            this.$scripts.find('[name="script"]').prop('checked', checked);
        },

        run: function (event) {
            event.preventDefault();
            var request = new XMLHttpRequest();
            request.addEventListener("progress", _.bind(function (event) {
                this.$out.append(event.target.responseText);
            }, this));
            request.addEventListener("loadend", _.bind(function () {
                this.$out.append('finished');
            }, this));
            this.$out.text('start...\n');
            request.open('POST', this.$form.attr('action'));
            request.send(new FormData(this.$form[0]));
            return false;
        },

        reload: function () {
            var url = '/libs/composum/nodes/commons/components/security.scripts.html?_charset_=UTF-8';
            var category = this.$category.serialize();
            if (category) {
                url += '&' + category;
            }
            core.getHtml(url, _.bind(function (content) {
                this.$scripts.html(content);
            }, this));
        }
    });

    $(document).ready(function () {
        core.getView('.' + setup.const.css.base, setup.View);
    });

})(CPM.namespace('nodes.commons.setup'), CPM.core);
