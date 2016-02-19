(function (core) {
    'use strict';

    core.usermanagement = core.usermanagement || {};

    (function (usermanagement) {


        usermanagement.Query = Backbone.View.extend({

            initialize: function (options) {
                this.$form = this.$('.query-actions form');
                this.$queryInput = this.$('.query-actions form input');
                this.$execButton = this.$('.query-actions .exec');
                this.$resultTable = this.$('.query-result table');
                this.$form.on('submit', _.bind(this.executeQuery, this));
                this.$execButton.on('click.query', _.bind(this.executeQuery, this));
            },

            executeQuery: function (event) {
                event.preventDefault();
                this.$resultTable.html('<tbody><tr><td class="pulse"><i class="fa fa-spinner fa-pulse"></i></td></tr></tbody>');
                var query = this.$queryInput.val();
                return false;
            }


        });

        usermanagement.query = core.getView('#usermanagement-query .query-panel', usermanagement.Query);


    })(core.usermanagement);

})(window.core);
