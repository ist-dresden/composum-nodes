(function(core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function(usermanagement) {

        usermanagement.getAddMemberDialog = function () {
            return core.getView('#add-member-dialog', usermanagement.AddMemberDialog);
        };

        usermanagement.AddMemberDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$authorizable = this.$('input[name="authorizable"]');
                this.group = core.getWidget(this.el, 'input[name="group"]', core.components.TextFieldWidget);
                this.$('button.create').click(_.bind(this.addMember, this));
                this.$el.on('shown.bs.modal', function () {
                    $(this).find('input[name="authorizable"]').focus();
                });
                this.$authorizable.attr('autocomplete', 'off');
                this.$authorizable.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        core.getJson('/bin/cpm/usermanagement.authorizables.json/' + query, function (data) {
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

            setGroup: function (group) {
                this.group.setValue(group);
            },

            addMember: function (event) {
                event.preventDefault();
                var serializedData = this.form.$el.serialize();
                core.ajaxPost(
                    "/bin/cpm/usermanagement.addtogroup.json",
                    serializedData,
                    {
                        dataType: 'post'
                    },
                    _.bind(function(result) {
                        this.hide();
                    }, this),
                    _.bind(function(result) {
                        this.hide();
                        core.alert('danger', 'Error', 'Error adding authorizable to group', result);
                    }, this));
                return false;
            }
        });


    })(core.usermanagement);

})(window.core);
