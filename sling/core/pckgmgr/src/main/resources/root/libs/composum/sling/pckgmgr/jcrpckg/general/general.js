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
                this.$('.display-toolbar .build').click(_.bind(this.buildPackage, this));
                this.$('.display-toolbar .rewrap').click(_.bind(this.rewrapPackage, this));
                this.$('.display-toolbar .upload').click(_.bind(pckgmgr.treeActions.uploadPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .create').click(_.bind(pckgmgr.treeActions.createPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .delete').click(_.bind(pckgmgr.treeActions.deletePackage, pckgmgr.treeActions));
                this.$('.display-toolbar .refresh').click(_.bind(this.refresh, this));
                this.$feedback.find('.close').click(_.bind(this.closeFeedback, this));
                this.$logOutput = this.$feedback.find('.feedback-display table');
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

            buildPackage: function (event) {
                if (event) {
                    event.preventDefault();
                }
            },

            rewrapPackage: function (event) {
                if (event) {
                    event.preventDefault();
                }
            },

            refresh: function (event) {
                if (event) {
                    event.preventDefault();
                }
                pckgmgr.detailView.reload();
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
            },

            callAndPoll: function (title, url, onSuccess, onError) {
                this.openFeedback();
                this.$title.text(title);
                this.$logOutput.html('');
                var path = pckgmgr.getCurrentPath();
                core.ajaxPoll('POST', url + path,
                    _.bind(function (xhr, snippet) {
                        this.logAppend(snippet);
                    }, this), _.bind(function (xhr, snippet) {
                        this.logAppend(snippet);
                        if (_.isFunction(onSuccess)) {
                            onSuccess(xhr);
                        }
                    }, this), _.bind(function (xhr, snippet) {
                        this.logAppend(snippet);
                        if (_.isFunction(onError)) {
                            onError(xhr);
                        }
                    }, this))
            }
        });

    })(core.pckgmgr);

})(window.core);
