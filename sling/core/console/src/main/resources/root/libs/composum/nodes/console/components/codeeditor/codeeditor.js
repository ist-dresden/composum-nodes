/**
 *
 *
 */
(function (core) {
    'use strict';

    core.console = core.console || {};

    (function (console) {

        console.CodeEditorPage = Backbone.View.extend({

            initialize: function (options) {
                this.path = this.$el.data('path');
                this.type = this.$el.data('type');
                this.editor = core.getWidget(this.el, '.widget.code-editor-widget', core.components.CodeEditorWidget);

                this.$('button.save').click(_.bind(this.save, this));
                this.$('.code-editor-widget_title').text(this.path);

                this.editor.$editor.attr('data-path', this.path);
                this.editor.$editor.attr('data-type', this.type);
                this.editor.initEditor();
                this.editor.setSaveCommand(_.bind(function (editor) {
                    this.save();
                }, this));

                core.unauthorizedDelegate = console.authorize;
            },

            save: function (event, onSuccess) {
                this.editor.saveText();
            }
        });

        core.getView('body.composum-nodes-components-codeeditor_page', console.CodeEditorPage);

    })(core.console);

})(window.core);
