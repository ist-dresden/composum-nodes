/**
 *
 *
 */
(function(core) {
    'use strict';

    core.components = core.components || {};

(function(components) {

    /**
     * the 'code-editor-widget'
     *
     */
    components.CodeEditorWidget = Backbone.View.extend({

        initialize: function(options) {
            this.$editor = this.$('.code-editor');
            this.initEditor(_.bind(function(data) {
                this.ace.setReadOnly(true);
                this.ace.navigateFileStart();
            }, this));
            this.$('.editor-toolbar .start-editing').click(_.bind(this.openEditDialog, this));
        },

        initEditor: function(onSuccess) {
            this.ace = ace.edit(this.$editor[0]);
            this.ace.setTheme('ace/theme/clouds');
            var type = this.$editor.attr('data-type');
            if (type) {
                this.ace.getSession().setMode('ace/mode/' + type);
            }
            this.loadText(onSuccess);
        },

        openEditDialog: function() {
            this.dialog = core.getView('#text-edit-dialog', components.CodeEditorDialog);
            this.dialog.show();
            this.dialog.setSource(this);
        },

        getPath: function() {
            return this.$editor.attr("data-path");
        },

        /**
         * loads the text data (jcr:data property) of the node referenced by the 'data-path' attribute
         * of the editor DOM element ($...('.text-editor .code-editor')) into the editor
         * @param event the current event object (used if target is undefined)
         * @param target the editor DOM element (can be undefined - than the events target is used to find it)
         * @param onSuccess the success callback handler (function(event,editor,data))
         */
        loadText: function(onSuccess){
            var path = this.getPath();
            if (path) {
                $.ajax({
                    url: "/bin/core/property.bin" + path,
                    contentType: 'text/plain;charset=UTF-8',
                    cache: false,
                    success: _.bind (function (data) {
                        this.ace.setValue(data);
                        this.ace.clearSelection();
                        if (_.isFunction(onSuccess)) {
                            onSuccess(data);
                        }
                    }, this),
                    error: _.bind (function (result) {
                        core.alert('danger', 'Error', 'Error on loading text', result);
                    }, this)
                });
            }
        },

        saveText: function(onSuccess){
            var path = this.getPath();
            if (path) {
                $.ajax({
                    url: "/bin/core/property.bin" + path,
                    data: this.ace.getValue(),
                    contentType: 'text/plain;charset=UTF-8',
                    async: true,
                    type: 'PUT',
                    complete: _.bind (function (result, x, y) {
                        if (result.status == 200) {
                            if (_.isFunction(onSuccess)) {
                                onSuccess(result);
                            }
                        } else {
                            core.alert('danger', 'Error', 'Error on updating text', result);
                        }
                    }, this)
                });
            }
        },

        reset: function() {
            this.ace.setValue('');
        }
    });

    components.CodeEditorDialog = components.Dialog.extend({

        initialize: function(options) {
            core.components.Dialog.prototype.initialize.apply(this, [options]);
            this.editor = core.getWidget(this.el, '.widget.code-editor-widget', components.CodeEditorWidget);
            // initialize the dialogs toolbar and buttons
            this.$('button.save').click(_.bind(function(event) {
                this.cursor = this.editor.ace.getCursorPosition();
                this.editor.saveText(_.bind (function(result) {
                    if (this.source) {
                        this.source.loadText(_.bind (function(data){
                            this.source.ace.navigateTo(this.cursor.row,this.cursor.column);
                            this.source.ace.scrollToRow(Math.max(this.cursor.row-2,0));
                        }, this));
                    }
                    this.hide();
                }, this));
            }, this));
        },

        setSource: function(source) {
            this.source = source;
            // initialize the dialog with the templates data
            var path = this.source.getPath();
            this.$('.modal-title').text(path);
            this.editor.$editor.attr('data-path',path);
            this.editor.$editor.attr('data-type',this.source.$editor.attr('data-type'));
            this.cursor = this.source.ace.getCursorPosition();
            // display the editor in the modal dialog
            this.$el.unbind('shown.bs.modal').on('shown.bs.modal', _.bind (function(){
                // initialize the editor instance and load the data
                this.editor.initEditor(_.bind (function(data){
                    this.editor.ace.setReadOnly(false);
                    this.editor.ace.navigateTo(this.cursor.row,this.cursor.column);
                    this.editor.ace.scrollToRow(Math.max(this.cursor.row-2,0));
                    this.editor.ace.focus();
                }, this));
            }, this));
            this.show();
        },

        reset: function() {
            this.editor.reset();
        }
    });

})(core.components);

})(window.core);
