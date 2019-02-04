/**
 * Favorites Overlay behaviour
 */

(function (browser) {
    'use strict';

    browser.Navigation = Backbone.View.extend({

        initialize: function (options) {
            this.verticalSplit = core.getWidget(this.$el, this.el, core.components.VerticalSplitPane);
            this.verticalSplit.$el.on('splitpaneresize', _.bind(this.onSplitResize, this));
            if (core.console.getProfile().get('navigation', 'favorites')) {
                this.toggleFavorites();
            }
        },

        onSplitResize: function (event) {
            event.preventDefault();
            event.stopPropagation();
            if (this.$el.hasClass('favorites-open')) {
                core.console.getProfile().set('navigation', 'split', this.verticalSplit.getPosition());
            }
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
                        core.console.getProfile().get('navigation', 'split', 400)));
                core.console.getProfile().set('navigation', 'favorites', true);
            } else {
                this.$el.removeClass('favorites-open').addClass('favorites-closed');
                core.console.getProfile().set('navigation', 'favorites', false);
            }
        }
    });

    browser.navigation = core.getView('#browser-nav-split', browser.Navigation);

})(window.core.browser);
