/**
 *
 *
 */
(function (core) {
    'use strict';

    core.pckgmgr = core.pckgmgr || {};

    (function (pckgmgr) {

        pckgmgr.JcrPackageTab = core.console.DetailTab.extend({

            initialize: function (options) {
                core.console.DetailTab.prototype.initialize.apply(this, [options]);
                this.$default = this.$('.default-aspect');
                this.$feedback = this.$('.feedback-aspect');
                this.$title = this.$feedback.find('.title');
                this.$output = this.$feedback.find('.feedback-display table');
                this.$('.display-toolbar .edit').click(_.bind(this.editPackage, this));
                this.$('.display-toolbar .install').click(_.bind(this.installPackage, this));
                this.$('.display-toolbar .build').click(_.bind(this.buildPackage, this));
                this.$('.display-toolbar .rewrap').click(_.bind(this.rewrapPackage, this));
                this.$('.display-toolbar .upload').click(_.bind(pckgmgr.treeActions.uploadPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .create').click(_.bind(pckgmgr.treeActions.createPackage, pckgmgr.treeActions));
                this.$('.display-toolbar .delete').click(_.bind(pckgmgr.treeActions.deletePackage, pckgmgr.treeActions));
                this.$('.display-toolbar .refresh').click(_.bind(this.refresh, this));
                this.$feedback.find('.close').click(_.bind(this.closeFeedback, this));
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
                this.callAndPoll('Install Package...', '/bin/core/package.install.html',
                    undefined, _.bind(function () {
                        this.logAppend('<tr class="error"><td colspan="3">Package install failed.</td></tr>');
                    }, this));
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
                this.$output.html('');
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
            },

            logAppend: function (data) {
                if (data) {
                    var $scrollPane = this.$output.parent();
                    var vheight = $scrollPane.height();
                    var height = this.$output[0].scrollHeight;
                    var autoscroll = ($scrollPane.scrollTop() > height - vheight - 30);
                    this.$output.append(data);
                    if (autoscroll) {
                        height = this.$output[0].scrollHeight;
                        $scrollPane.scrollTop(height - vheight);
                    }
                }
            }
        });

    })(core.pckgmgr);

})(window.core);
