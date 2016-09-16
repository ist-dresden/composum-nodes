(function (core) {
    'use strict';

    core.components = core.components || {};

    (function (components) {

        components.const = _.extend(components.const || {}, {
            richtext: {
                cssBase: 'richtext-widget',
                editorSelector: '.rich-editor',
                defaultOptions: {
                    btns: [
                        ['bold', 'italic', 'underline', 'strikethrough'],
                        ['superscript', 'subscript'], ['removeformat'],
                        'btnGrp-lists',
                        ['link', 'unlink']
                    ],
                    removeformatPasted: true
                }
            }
        });

        components.RichTextWidget = Backbone.View.extend({

            initialize: function (options) {
                this.$editor = this.richText();
                this.$editor.trumbowyg(components.const.richtext.defaultOptions);
                var style = this.$el.data('style');
                if (style) {
                    this.$('.trumbowyg-editor').attr('style', style);
                }
                this.$editor.on('tbwchange', _.bind(this.onChange, this));
                //this.$editor.on('summernote.change', _.bind(this.onChange, this));
            },

            getValue: function () {
                return this.$editor.trumbowyg('html');
                //return this.$editor.summernote('code');
            },

            setValue: function (value, triggerChange) {
                this.$editor.trumbowyg('html', value);
                //this.$editor.summernote('code', value);
                if (triggerChange) {
                    this.$el.trigger('change');
                }
            },

            reset: function () {
                this.$editor.trumbowyg('empty');
                //this.setValue('');
            },

            onChange: function () {
                this.$el.trigger('change');
            },

            richText: function () {
                return this.$el.is(components.const.richtext.editorSelector)
                    ? this.$el
                    : this.$(components.const.richtext.editorSelector);
            }
        });

        /**
         * trumbowyg editor customizing
         */
        components.trumbowyg = {

            openModalWidgets: function (dialogUri, values, cmd) {
                var t = this;

                core.getHtml(dialogUri, function (html) {
                    var $dialog = t.openModal('Content Link', html),
                        form = core.getWidget($dialog[0], 'form', core.components.FormWidget),
                        formWidgets = {};
                    $dialog.addClass('composum-widgets-richtext_link-dialog');
                    components.setUp(form.el);

                    form.$('[name]').each(function () {
                        var name = $(this).attr('name');
                        if (name) {
                            var widget = core.widgetOf(this);
                            if (widget) {
                                var value = values[name];
                                widget.setValue(value ? value : undefined);
                                formWidgets[name] = widget;
                            }
                        }
                    });

                    $dialog.on('tbwconfirm', function () {

                        if (form.validate()) {

                            for (var name in formWidgets) {
                                var widget = formWidgets[name];
                                values[name] = widget.getValue();
                            }

                            if (cmd(values)) {
                                t.syncCode();
                                t.$c.trigger('tbwchange');
                                t.closeModal();
                                $(this).off('tbwconfirm');
                            }
                        }
                    });

                    $dialog.one('tbwcancel', function () {
                        $(this).off('tbwconfirm');
                        t.closeModal();
                    });
                });
            }
        };

        $.extend(true, $.trumbowyg, {

            plugins: {
                contentLink: {
                    init: function (trumbowyg) {

                        var btnDef = {
                            fn: function () {

                                var openLinkDialog = function () {
                                    var documentSelection = trumbowyg.doc.getSelection(),
                                        node = documentSelection.focusNode,
                                        values = {};

                                    while (['A', 'DIV'].indexOf(node.nodeName) < 0) {
                                        node = node.parentNode;
                                    }

                                    if (node && node.nodeName === 'A') {
                                        var $a = $(node);
                                        values.url = $a.attr('href');
                                        values.title = $a.attr('title');
                                        values.target = $a.attr('target');
                                        var range = trumbowyg.doc.createRange();
                                        range.selectNode(node);
                                        documentSelection.addRange(range);
                                    }

                                    trumbowyg.saveRange();
                                    values.text = trumbowyg.getRangeText();

                                    components.trumbowyg.openModalWidgets.apply(trumbowyg, [
                                        '/libs/composum/nodes/commons/components/richtext/link/dialog.html',
                                        values,
                                        insertLinkCallback
                                    ]);
                                };

                                var insertLinkCallback = function (v) {

                                    trumbowyg.restoreRange();

                                    var link = $(['<a href="', v.url, '">', v.text, '</a>'].join(''));
                                    if (v.title.length > 0) {
                                        link.attr('title', v.title);
                                    }
                                    if (v.target.length > 0) {
                                        link.attr('target', v.target);
                                    }

                                    trumbowyg.range.deleteContents();
                                    trumbowyg.range.insertNode(link[0]);
                                    return true;
                                };

                                openLinkDialog();
                            }
                        };

                        // replace default 'link' behaviour by our extension
                        trumbowyg.addBtnDef('link', btnDef);
                    }
                }
            }
        });

    })(core.components);

})(window.core);
