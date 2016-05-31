/**
 * Favorites Overlay behaviour
 */

(function (browser) {
    'use strict';

    browser.Navigation = Backbone.View.extend({

        initialize: function (options) {
            this.verticalSplit = core.getWidget(this.$el, this.el, core.components.VerticalSplitPane);
            this.verticalSplit.$el.on('resize', _.bind(this.onSplitResize, this));
        },

        onSplitResize: function (event) {
            event.preventDefault();
            event.stopPropagation();
            core.console.getProfile().set('browser', 'navigation', this.verticalSplit.getPosition());
        },

        toggleFavorites: function (event) {
            if (event) {
                event.preventDefault();
                event.stopPropagation();
            }
            if (this.$el.hasClass('favorites-closed')) {
                this.$el.removeClass('favorites-closed').addClass('favorites-open');
                this.verticalSplit.setPosition(
                    this.verticalSplit.checkPosition(
                        core.console.getProfile().get('favorites', 'split', 200)));
                core.console.getProfile().set('browser', 'navigation', 'favorites');
            } else {
                this.$el.removeClass('favorites-open').addClass('favorites-closed');
                core.console.getProfile().set('browser', 'navigation', undefined);
            }
        }
    });

    browser.navigation = core.getView('#browser-nav-split', browser.Navigation);

})(window.core.browser);
