/**
 *
 *
 */
(function (core) {
    'use strict';

    core.nodes = core.nodes || {};

    (function (nodes) {

        nodes.getCreateNodeDialog = function () {
            return core.getView('#node-create-dialog', nodes.CreateNodeDialog);
        };

        nodes.getMoveNodeDialog = function () {
            return core.getView('#node-move-dialog', nodes.MoveNodeDialog);
        };

        nodes.getRenameNodeDialog = function () {
            return core.getView('#node-rename-dialog', nodes.RenameNodeDialog);
        };

        nodes.getCopyNodeDialog = function () {
            return core.getView('#node-copy-dialog', nodes.CopyNodeDialog);
        };

        nodes.getNodeMixinsDialog = function () {
            return core.getView('#node-mixins-dialog', nodes.NodeMixinsDialog);
        };

        nodes.getDeleteNodeDialog = function () {
            return core.getView('#node-delete-dialog', nodes.DeleteNodeDialog);
        };

        nodes.getUploadNodeDialog = function () {
            return core.getView('#node-upload-dialog', nodes.UploadNodeDialog);
        };

        nodes.getUpdateFileDialog = function () {
            return core.getView('#file-update-dialog', nodes.UpdateFileDialog);
        };

        nodes.CreateNodeDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$panel = this.$('.form-panel');
                this.$path = this.$('input[name="path"]');
                this.$type = this.$('input[name="type"]');
                this.$indexType = this.$('input[name="indexType"]');
                this.$name = this.$('input[name="name"]');
                this.$file = this.$('input[name="file"]');
                this.$type.on('change.type', _.bind(this.typeChanged, this));
                this.$file.on('change.file', _.bind(this.fileChanged, this));
                this.form.onsubmit = _.bind(this.createNode, this);
                this.$('button.create').click(_.bind(this.createNode, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="type"]').focus();
                });
            },

            initParentPath: function (path) {
                this.$path.val(path);
            },

            createNode: function (event) {
                event.preventDefault();
                var parentPath = this.$path.val();
                var newNodeName = this.$name.val();
                var newNodePath = core.buildContentPath(parentPath, newNodeName);
                if (this.form.isValid()) {
                    this.submitForm(function (result) {
                        $(document).trigger("path:inserted", [parentPath, newNodeName]);
                        $(document).trigger("path:select", [newNodePath]);
                    });
                } else {
                    this.alert('danger', 'a parent path, type and name must be specified');
                }
                return false;
            },

            typeChanged: function () {
                var value = this.$type.val();
                if (value && value.match(/(file|[Rr]esource)$/)) {
                    this.setTypeView('binary');
                } else if (value && value.match(/^oak:QueryIndexDefinition/)) {
                    this.setTypeView('index');
                } else if (value && !value.match(/^(nt|rep):/)) {
                    this.setTypeView('sling');
                } else if (value && value.match(/^(nt|sling):linked.*/)) {
                    this.setTypeView('linked');
                } else {
                    this.setTypeView('default');
                }
            },

            /**
             * selects the type class of the form panel to switch to the appropriate dialog variation
             */
            setTypeView: function (type) {
                this.$panel.removeClass('default binary sling linked index'); // remove all type classes
                this.$panel.addClass(type); // set the specified type class at the form panel
            },

            fileChanged: function () {
                var fileWidget = this.widgetOf(this.$file);
                var nameWidget = this.widgetOf(this.$name);
                var value = fileWidget.getValue();
                if (value) {
                    var name = nameWidget.getValue();
                    if (!name) {
                        var match = /^(.*[\\\/])?([^\\\/]+)$/.exec(value);
                        nameWidget.setValue([match[2]]);
                    }
                }
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
                this.typeChanged();
            }
        });

        nodes.NodeMixinsDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$path = this.$('input[name="path"]');
                this.multi = core.getWidget(this.el, 'div.multi-form-widget', core.components.MultiFormWidget);
                this.form.onsubmit = _.bind(this.saveNode, this);
                this.$('button.submit').click(_.bind(this.saveNode, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="value"]').focus();
                });

            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            setWidgetValue: function (element, value) {
                var widget = core.widgetOf(element, this);
                if (widget && _.isFunction(widget.setValue)) {
                    widget.setValue(value);
                } else {
                    element.val(value);
                }
            },

            setNode: function (node) {
                this.$path.val(node.path);
                core.getJson("/bin/cpm/nodes/node.mixins.json" + node.path,
                    _.bind(function (mixins) {
                        this.multi.reset(mixins.length);
                        var values = this.multi.$('input[name="value"]');
                        for (var i = 0; i < mixins.length; i++) {
                            this.setWidgetValue($(values[i]), mixins[i]);
                        }
                    }, this));
            },

            saveNode: function (event) {
                event.preventDefault();
                function mixinValues(arrayOfElements) {
                    var stringValues = [];
                    for (var i = 0; i < arrayOfElements.length; i++) {
                        var mixinName = $(arrayOfElements[i]).val();
                        if (mixinName != '') {
                            stringValues[i] = mixinName;
                        }
                    }
                    return stringValues;
                }

                var mixinStrings = mixinValues(this.$('input[name="value"]'));
                var path = this.$path.val();

                core.ajaxPut("/bin/cpm/nodes/property.put.json" + path, JSON.stringify({
                    name: 'jcr:mixinTypes',
                    multi: true,
                    value: mixinStrings,
                    type: 'Name'
                }), {
                    dataType: 'json'
                }, _.bind(function (result) {
                    $(document).trigger('path:changed', [path]);
                    this.hide();
                }, this), _.bind(function (result) {
                    core.alert('danger', 'Error', 'Error on updating mixin entries', result);
                }, this));

                return false;
            }
        });

        nodes.CopyNodeDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.path = core.getWidget(this.el, '.path.path-widget', core.components.PathWidget);
                this.$node = this.$('input[name="node"]');
                this.newname = core.getWidget(this.el, 'input[name="newname"]', core.components.TextFieldWidget);
                this.$('button.copy').click(_.bind(this.copyNode, this));
                this.$el.on('shown.bs.modal', _.bind(function () {
                    this.newname.focus();
                    this.newname.selectAll();
                }, this));
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            setNodePath: function (nodePath) {
                var nodeName = core.getNameFromPath(nodePath);
                this.newname.setValue(nodeName);
                this.$node.val(nodePath);
            },

            setTargetPath: function (path) {
                this.path.setValue(path);
            },

            copyNode: function (event) {
                event.preventDefault();
                var parentPath = this.path.getValue();
                var newNodeName = this.newname.getValue();
                core.ajaxPut("/bin/cpm/nodes/node.copy.json" + parentPath, JSON.stringify({
                    path: this.$node.val(),
                    name: newNodeName
                }), {
                    dataType: 'json'
                }, _.bind(function (result) {
                    $(document).trigger('path:inserted', [parentPath, newNodeName]);
                    this.hide();
                }, this), _.bind(function (result) {
                    core.alert('danger', 'Error', 'Error on copying node', result);
                }, this));
                return false;
            }
        });

        nodes.MoveNodeDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$node = this.$('input[name="target-node"]');
                this.$path = this.$('input[name="path"]');
                this.$name = this.$('input[name="name"]');
                this.$index = this.$('input[name="index"]');
                this.$('button.move').click(_.bind(this.moveNode, this));
            },

            setNode: function (node) {
                this.$node.val(node.path);
                this.$path.val(core.getParentPath(node.path));
                this.$name.val(node.name);
            },

            setValues: function (draggedNode, dropTarget, index) {
                this.$node.val(draggedNode.path);
                this.$path.val(dropTarget.path);
                this.$name.val(draggedNode.name);
                this.$index.val(index);
            },

            moveNode: function (event) {
                event.preventDefault();
                if (this.$form.isValid()) {
                    var oldPath = this.$node.val();
                    var name = this.$name.val();
                    var targetPath = this.$path.val();
                    var index = this.$index.val();
                    if (!index && index != 0) {
                        index = -1
                    }
                    var newPath = core.buildContentPath(targetPath, name);
                    this.submitPUT(
                        'move node',
                        '/bin/cpm/nodes/node.move.json' + core.encodePath(oldPath), {
                            name: name,
                            path: targetPath,
                            index: index
                        },
                        function () {
                            $(document).trigger('path:moved', [oldPath, newPath]);
                        });
                } else {
                    this.alert('danger', 'a valid target path and the node must be specified');
                }
            }
        });

        nodes.RenameNodeDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$path = this.$('input[name="path"]');
                this.$name = this.$('input[name="name"]');
                this.$newname = core.getWidget(this.el, 'input[name="newname"]', core.components.TextFieldWidget);
                this.$('button.rename').click(_.bind(this.renameNode, this));
                this.form.onsubmit = _.bind(this.renameNode, this);
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="newname"]').focus();
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            setNode: function (node, newName) {
                this.$path.val(core.getParentPath(node.path));
                this.$name.val(node.name);
                this.$newname.setValue(newName ? newName : node.name);
            },

            renameNode: function (event) {
                event.preventDefault();
                var path = this.$path.val();
                var oldName = this.$name.val();
                var newName = this.$newname.getValue();
                var oldPath = core.buildContentPath(path, oldName);
                var newPath = core.buildContentPath(path, newName);
                core.ajaxPut("/bin/cpm/nodes/node.move.json" + oldPath,
                    JSON.stringify({
                        name: newName,
                        path: path
                    }), {
                        dataType: 'json'
                    },
                    _.bind(function (result) {
                        $(document).trigger('path:moved', [oldPath, newPath]);
                        this.hide();
                    }, this),
                    _.bind(function (result) {
                        this.alert('danger', 'Error on renaming node', result);
                    }, this));
                return false;
            }
        });

        nodes.DeleteNodeDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.path = core.getWidget(this.el, '.path.path-widget', core.components.PathWidget);
                this.$smart = this.$('input.smart');
                this.$('button.delete').click(_.bind(this.deleteNode, this));
                this.form.onsubmit = _.bind(this.deleteNode, this);
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="path"]').focus();
                });
            },

            show: function (initView, callback) {
                if (this.doItSmart()) {
                    this.initView = initView;
                    this.callback = callback;
                    this.onShown();
                    // check 'smart' once more because it can be reset during 'initView'
                    if (this.doItSmart()) {
                        this.deleteNode();
                    } else {
                        this.$el.modal('show');
                    }
                } else {
                    core.components.Dialog.prototype.show.apply(this, [initView, callback]);
                }
            },

            setPath: function (path) {
                this.path.setValue(path);
            },

            deleteNode: function (event) {
                if (event) {
                    event.preventDefault();
                }
                if (this.form.isValid()) {
                    var path = this.path.getValue();
                    core.ajaxDelete("/bin/cpm/nodes/node.json" + core.encodePath(path),
                        {},
                        _.bind(function (result) {
                            $(document).trigger('path:deleted', [path]);
                            this.hide();
                        }, this),
                        _.bind(function (result) {
                            // if event is present than this is triggered by the dialog
                            if (!event && this.doItSmart()) {
                                this.show(_.bind(function () {
                                    this.doAlert(result);
                                }, this), this.callback)
                            } else {
                                this.doAlert(result);
                            }
                        }, this));
                } else {
                    this.alert('danger', 'a valid node path must be specified');
                }
                return false;
            },

            doItSmart: function () {
                return this.$smart.prop('checked');
            },

            setSmart: function (value) {
                this.$smart.prop('checked', value);
            },

            doAlert: function (result) {
                this.alert('danger', 'Error on delete node!', result);
            }
        });

        nodes.UploadNodeDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$panel = this.$('.form-panel');
                this.$path = this.$('input[name="path"]');
                this.$name = this.$('input[name="name"]');
                this.$file = this.$('input[name="file"]');
                this.$file.on('change.file', _.bind(this.fileChanged, this));
                this.$('button.upload').click(_.bind(this.uploadNode, this));
            },

            initDialog: function (path, name) {
                this.$path.val(path);
                this.$name.val(name);
            },

            uploadNode: function (event) {
                event.preventDefault();
                if (this.$form.isValid()) {
                    var parentPath = this.$path.val();
                    var newNodeName = this.$name.val();
                    this.submitForm(function () {
                        $(document).trigger('path:inserted', [parentPath, newNodeName]);
                    });
                } else {
                    this.alert('danger', 'a parent path and file must be specified');
                }
                return false;
            },

            fileChanged: function () {
                var fileWidget = this.widgetOf(this.$file);
                var nameWidget = this.widgetOf(this.$name);
                var value = fileWidget.getValue();
                if (value) {
                    var name = nameWidget.getValue();
                    if (!name) {
                        var match = /^(.*[\\\/])?([^\\\/]+)(\.json)$/.exec(value);
                        if (match) {
                            nameWidget.setValue([match[2]]);
                        } else {
                            match = /^(.*[\\\/])?([^\\\/]+)$/.exec(value);
                            if (match) {
                                nameWidget.setValue([match[2]]);
                            }
                        }
                    }
                }
            }
        });

        nodes.UpdateFileDialog = core.components.Dialog.extend({

            initialize: function (options) {
                this.profile = {
                    adjustLastModified: true
                };
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$panel = this.$('.form-panel');
                this.$path = this.$('input[name="path"]');
                this.$file = this.$('input[name="file"]');
                this.$adjustLastModified = this.$('input[name="adjustLastModified"]');
                this.$('button.update').click(_.bind(this.updateFile, this));
            },

            initDialog: function (path) {
                this.$path.val(path);
                this.profile = core.console.getProfile().get('browser', 'updateFileDialog', this.profile);
                this.$adjustLastModified.prop('checked', this.profile.adjustLastModified);
            },

            updateFile: function (event) {
                event.preventDefault();
                var path = this.$path.val();
                this.submitForm(_.bind(function () {
                    this.profile.adjustLastModified = this.$adjustLastModified.prop('checked');
                    core.console.getProfile().set('browser', 'updateFileDialog', this.profile);
                    $(document).trigger('path:changed', [path]);
                }, this));
                return false;
            }
        });

    })(core.nodes);

})(window.core);
