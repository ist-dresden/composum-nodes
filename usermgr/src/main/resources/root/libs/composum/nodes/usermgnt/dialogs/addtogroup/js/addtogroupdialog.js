(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, core) {

        usermanagement.getAddToGroupDialog = function () {
            return core.getView('#add-to-group-dialog', usermanagement.AddToGroupDialog);
        };

        usermanagement.AddToGroupDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.authorizable = core.getWidget(this.el, 'input[name="authorizable"]', core.components.TextFieldWidget);
                this.$group = this.$('input[name="group"]');
                this.$('button.create').click(_.bind(this.addToGroup, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="group"]').focus();
                });
                this.$group.attr('autocomplete', 'off');
                this.$group.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        core.getJson('/bin/cpm/usermanagement.groups.json/' + query, function (data) {
                            var list = [];
                            for (var i = 0; i < data.length; i++) {
                                list.push(data[i].id);
                            }
                            callback(list);
                        });
                    }
                });
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            setUser: function (user) {
                this.authorizable.setValue(user);
            },

            addToGroup: function (event) {
                event.preventDefault();
                var serializedData = this.form.$el.serialize();
                core.ajaxPost(
                    "/bin/cpm/usermanagement.addtogroup.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function (result) {
                        this.hide();
                    }, this),
                    _.bind(function (result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error adding authorizable to group', result);
                    }, this));
                return false;
            }
        });

    })(CPM.nodes.usermanagement, CPM.core);

})();
