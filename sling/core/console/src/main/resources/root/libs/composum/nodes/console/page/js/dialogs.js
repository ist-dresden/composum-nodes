'use strict';
/**
 *
 *
 */
(function (core) {

    core.console = core.console || {};

    (function (console) {

        console.getApprovalDialog = function () {
            return core.getView('#approval-dialog', console.ApprovalDialog);
        };

        console.getPurgeAuditDialog = function () {
            return core.getView('#purge-audit-dialog', console.PurgeAuditDialog);
        };

        console.ApprovalDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.$form = this.$('form');
                this.$title = this.$('.modal-header .modal-title');
                this.$content = this.$('.modal-body .content');
                this.$form.on('submit', _.bind(function (event) {
                    event.preventDefault();
                    this.hide();
                    return false;
                }, this));
            },

            initDialog: function (title, content) {
                this.$title.text(title);
                this.$content.html(content);
            }
        });

        console.PurgeAuditDialog = core.components.Dialog.extend({

            initialize: function (options) {
                core.components.Dialog.prototype.initialize.apply(this, [options]);
                this.form = core.getWidget(this.el, 'form.widget-form', core.components.FormWidget);
                this.$topic = this.$('input[name="event.job.topic"]');
                this.$reference = this.$('input[name="reference"]');
                this.$keep = this.$('input[name="keep"]');
                this.form.$el.on('submit', _.bind(this.purgeAuditLog, this));
            },

            initDialog: function (topic, reference, keepDefault) {
                this.$topic.val(topic);
                this.$reference.val(reference);
                this.$keep.val(keepDefault);
            },

            purgeAuditLog: function (event) {
                event.preventDefault();
                if (this.form.isValid()) {
                    this.form.$el.attr('action', core.getContextUrl('/bin/cpm/core/jobcontrol.cleanup.json'));
                    this.submitForm(function (result) {
                    });
                } else {
                    this.alert('danger', 'topic and reference must be specified');
                }
                return false;
            }
        });

    })(core.console);

})(window.core);
