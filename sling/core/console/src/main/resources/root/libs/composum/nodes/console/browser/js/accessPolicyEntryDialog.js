/**
 *
 *
 */
(function (core) {
    'use strict';

    core.browser = core.browser || {};

    (function (browser) {


        browser.AccessPolicyEntryDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.rule = core.getWidget(this.el, '.rule .radio-group-widget', core.components.RadioGroupWidget);
                this.$principal = this.$('input[name="principal"]');
                this.$privilege = this.$('select[name="privilege"]');
                this.$restriction = this.$('select[name="restrictionKey"]');
                this.$('button.save').click(_.bind(this.saveACL, this));
                this.privilegeCombobox = core.getWidget(this.el, this.$privilege, core.components.SelectWidget);
                this.restrictionCombobox = core.getWidget(this.el, this.$restriction, core.components.SelectWidget);
                this.$principal.attr('autocomplete', 'off');
                this.$principal.typeahead({
                    minLength: 1,
                    source: function (query, callback) {
                        core.getJson('/bin/cpm/nodes/security.principals.json/' + query, function (data) {
                            callback(data);
                        });
                    }
                });
                this.$el.on('shown.bs.modal', _.bind(function () {
                    this.rule.setValue('allow');
                    this.loadSupportedPrivileges();
                    this.loadRestrictionNames();
                    this.$principal.focus();
                }, this));
            },

            reset: function () {
                core.components.Dialog.prototype.reset.apply(this);
            },

            loadSupportedPrivileges: function () {
                this.privilegeCombobox.$el.html('');
                core.getJson("/bin/cpm/nodes/security.supportedPrivileges.json" + browser.getCurrentPath(),
                    _.bind(function (privileges) {
                        for (var i = 0; i < privileges.length; i++) {
                            this.$privilege.append('<option value="' + privileges[i] + '">' + privileges[i] + '</option>');
                        }
                    }, this));
            },

            loadRestrictionNames: function () {
                this.restrictionCombobox.$el.html('');
                this.$restriction.append('<option value=""></option>');
                core.getJson("/bin/cpm/nodes/security.restrictionNames.json" + browser.getCurrentPath(),
                    _.bind(function (restrictionNames) {
                        for (var i = 0; i < restrictionNames.length; i++) {
                            this.$restriction.append('<option value="' + restrictionNames[i] + '">' + restrictionNames[i] + '</option>');
                        }
                    }, this));
                this.$restriction[0].selectedIndex = -1;
            },

            saveACL: function () {
                var path = browser.getCurrentPath();

                function privilegeValues(arrayOfSelects) {
                    var stringValues = [];
                    for (var i = 0; i < arrayOfSelects.length; i++) {
                        stringValues[i] = $(arrayOfSelects[i]).val();
                    }
                    return stringValues;
                }

                function restrictionValues(arrayOfSelects) {
                    var restrictionStrings = [];
                    for (var i = 0; i < arrayOfSelects.length; i++) {
                        var key = $(arrayOfSelects[i]).val();
                        if (key != '') {
                            var value = $(arrayOfSelects[i]).parent().find('input[name="restrictionValue"]').val();
                            restrictionStrings[i] = key + '=' + value;
                        }
                    }
                    return restrictionStrings;
                }

                var privilegeStrings = privilegeValues($('select[name="privilege"]'));
                var restrictionStrings = restrictionValues($('select[name="restrictionKey"]'));

                core.ajaxPut("/bin/cpm/nodes/security.accessPolicy.json" + path,
                    JSON.stringify({
                        principal: $(".form-control[name='principal']")[0].value,
                        allow: $(".form-control>div.allow input")[0].checked,
                        privileges: privilegeStrings,
                        restrictions: restrictionStrings,
                        path: path
                    }), {
                        dataType: 'json'
                    }, _.bind(function (result) {
                        this.hide();
                    }, this), _.bind(function (result) {
                        core.alert('danger', 'Error', 'Error on adding access policy entries', result)
                    }, this));
            }

        });

        browser.openAccessPolicyEntryDialog = function (callback) {
            var dialog = core.getView('#access-policy-entry-dialog', browser.AccessPolicyEntryDialog);
            dialog.show(undefined, callback);
        };


    })(core.browser);

})(window.core);
