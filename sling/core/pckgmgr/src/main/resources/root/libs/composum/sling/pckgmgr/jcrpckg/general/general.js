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
                this.$('.isplay-toolbar .edit').click(_.bind(this.editPackage, this));
                this.$('.isplay-toolbar .install').click(_.bind(this.installPackage, this));
                this.$('.isplay-toolbar .build').click(_.bind(this.buildPackage, this));
                this.$('.isplay-toolbar .rewrap').click(_.bind(this.rewrapPackage, this));
                this.$('.isplay-toolbar .upload').click(_.bind(this.uploadPackage, this));
                this.$('.isplay-toolbar .create').click(_.bind(this.createPackage, this));
                this.$('.isplay-toolbar .delete').click(_.bind(this.deletePackage, this));
                this.$('.isplay-toolbar .refresh').click(_.bind(this.refresh, this));
            },

            editPackage: function (event) {
                event.preventDefault();
            },

            installPackage: function (event) {
                event.preventDefault();
            },

            buildPackage: function (event) {
                event.preventDefault();
            },

            rewrapPackage: function (event) {
                event.preventDefault();
            },

            uploadPackage: function (event) {
                event.preventDefault();
            },

            createPackage: function (event) {
                event.preventDefault();
            },

            deletePackage: function (event) {
                event.preventDefault();
            },

            refresh: function (event) {
                event.preventDefault();
            }
        });

    })(core.pckgmgr);

})(window.core);
