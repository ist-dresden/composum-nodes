/**
 *
 *
 */
(function (core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.JcrPackageTab = core.console.JobControlTab.extend({

            jobTopic: 'com/composum/sling/core/pckgmgr/PackageJobExecutor',

            initialize: function (options) {
                core.console.JobControlTab.prototype.initialize.apply(this, [options]);
                this.$default = this.$('.default-aspect');
                this.$feedback = this.$('.feedback-aspect');
                this.$title = this.$feedback.find('.title');
                this.$('.display-toolbar .edit').click(_.bind(this.editPackage, this));
                this.$('.display-toolbar .install').click(_.bind(this.installPackage, this));
                this.$('.display-toolbar .assemble').click(_.bind(this.assemblePackage, this));
                this.$('.display-toolbar .uninstall').click(_.bind(this.uninstallPackage, this));
                this.$('.display-toolbar .upload').click(_.bind(pckgmgr.treeActions.uploadPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .create').click(_.bind(pckgmgr.treeActions.createPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .delete').click(_.bind(pckgmgr.treeActions.deletePackage, pckgmgr.treeActions));
                this.$('.display-toolbar .refresh').click(_.bind(this.refresh, this));
                this.$feedback.find('.close').click(_.bind(this.closeFeedback, this));
                this.$logOutput = this.$feedback.find('.feedback-display .log-output');
            },

            getCurrentPath: function () {
                return pckgmgr.getCurrentPath();
            },

            editPackage: function (event) {
                if (event) {
                    event.preventDefault();
                }
            },

            installPackage: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.startJob({
                    operation: 'install'
                });
            },

            assemblePackage: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.startJob({
                    operation: 'assemble'
                });
            },

            uninstallPackage: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.startJob({
                    operation: 'uninstall'
                });
            },

            refresh: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.reload();
            },

            jobStarted: function (job) {
                core.console.JobControlTab.prototype.jobStarted.apply(this, [job]);
                this.openFeedback();
            },
            
            jobSucceeded: function() {
                this.jobStopped();
            },

            closeFeedback: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.$feedback.addClass('hidden');
                this.$default.removeClass('hidden');
            },

            openFeedback: function (event) {
                if (event) {
                    event.preventDefault();
                }
                this.$default.addClass('hidden');
                this.$feedback.removeClass('hidden');
            }
        });

    })(core.pckgmgr);

})(window.core);
