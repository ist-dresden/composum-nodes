/**
 * Favorites Overlay behaviour
 */

(function(browser) {
    'use strict';

    browser.Favorites = Backbone.View.extend({

        initialize: function(options) {
            this.verticalSplit = core.getWidget(this.$el,
                '.split-pane.vertical-split', core.components.VerticalSplitPane);
            this.$favorites = this.$('.marked-nodes');
            this.$recently = this.$('.recently-used');
            this.$('button.toggle').on('click', _.bind(this.toggleView, this));
        },

        toggleView: function(event) {
            if (event) {
                event.preventDefault();
            }
            if (this.$el.hasClass('hidden')) {
                this.$el.removeClass('hidden');
                this.verticalSplit.setPosition(200);
            } else {
                this.$el.addClass('hidden');
            }
            return false;
        },

        isFavorite: function(path) {
            return this.getFavorite(path) !== undefined;
        },

        toggleFavorite: function(path) {
            if (!this.isFavorite(path)) {
                this.addFavorite(path);
            } else {
                this.removeFavorite(path);
            }
        },

        getFavorite: function(path) {
            var $item = this.$favorites.find('a[data-path="'+path+'"]');
            return $item && $item.length > 0 ? $item : undefined;
        },

        addFavorite: function(path) {
            if (!this.isFavorite(path)) {
            }
        },

        removeFavorite: function(path) {
            var $item = this.getFavorite(path);
            if ($item) {
            }
        },

        notifyUsage: function(path) {
        }
    });

    browser.favorites = core.getView('#favorites-overlay', browser.Favorites);

})(window.core.browser);
