/**
 * Favorites Overlay behaviour
 */

(function (browser) {
    'use strict';

    browser.Favorites = Backbone.View.extend({

        initialize: function (options) {
            this.$favorites = this.$('.marked-nodes');
            this.$favoritesList = this.$favorites.find('> ol');
            this.$recently = this.$('.used-recently');
            this.$recentlyList = this.$recently.find('> ol');
            this.$template = this.$('> .template a');
            this.$('.action-bar .favorites').click(_.bind(this.showFavoritesTab, this));
            this.$('.action-bar .history').click(_.bind(this.showHistoryTab, this));
            this.$('.action-bar .close').click(_.bind(browser.navigation.toggleFavorites, browser.navigation));
            this.$('.action-bar .clear-favorites').on('click', _.bind(this.clearFavorites, this));
            this.$('.action-bar .clear-recently').on('click', _.bind(this.clearHistory, this));
            $(document).on('favorite:toggle', _.bind(this.onToggleFavorite, this));
            $(document).on('path:selected', _.bind(this.onPathSelected, this));
            this.loadProfile();
            if (core.console.getProfile().get('navigation', 'tab') == 'history') {
                this.showHistoryTab();
            }
        },

        showFavoritesTab: function (event) {
            if (event) {
                event.preventDefault();
            }
            this.$el.removeClass('history').addClass('favorites');
            core.console.getProfile().set('navigation', 'tab', 'favorites');
        },

        showHistoryTab: function (event) {
            if (event) {
                event.preventDefault();
            }
            this.$el.removeClass('favorites').addClass('history');
            core.console.getProfile().set('navigation', 'tab', 'history');
        },

        onSelect: function (event) {
            event.preventDefault();
            event.stopPropagation();
            $(document).trigger("path:select", [event.data.path]);
        },

        isFavorite: function (path) {
            return this.getFavorite(path) !== undefined;
        },

        onToggleFavorite: function (event, path) {
            this.toggleFavorite(path);
        },

        toggleFavorite: function (path) {
            if (!this.isFavorite(path)) {
                this.addFavorite(path);
            } else {
                this.removeFavorite(path);
            }
        },

        getFavorite: function (path) {
            var $item = this.$favoritesList.find('a[data-path="' + path + '"]');
            return $item && $item.length > 0 ? $item : undefined;
        },

        addFavorite: function (path) {
            if (!this.isFavorite(path)) {
                var $items = this.$favoritesList.find('> a');
                var $next = undefined;
                for (var i = 0; i < $items.length; i++) {
                    var itemPath = $($items[i]).attr('data-path');
                    if (itemPath > path) {
                        $next = $($items[i]);
                        break;
                    }
                }
                var $favorite = this.createItem(path);
                if ($next) {
                    $next.before($favorite);
                } else {
                    this.$favoritesList.append($favorite);
                }
                this.saveProfile();
            }
        },

        removeFavorite: function (path) {
            var $item = this.getFavorite(path);
            if ($item) {
                $item.remove();
                this.saveProfile();
            }
        },

        onPathSelected: function (event, path) {
            this.notifyUsage(path);
            this.$favoritesList.children().removeClass('active');
            var $favorite = this.getFavorite(path);
            if ($favorite) {
                $favorite.addClass('active');
            }
        },

        notifyUsage: function (path) {
            var $item = this.$recentlyList.find('a[data-path="' + path + '"]');
            $item.each(function () {
                this.remove();
            });
            this.$recentlyList.prepend(this.createItem(path));
            var $listContent = this.$recentlyList.children();
            for (var i = $listContent.length; --i > 20;) {
                $($listContent[i]).remove();
            }
            this.saveProfile();
        },

        createItem: function (path) {
            var $item = this.$template.clone();
            $item.attr('data-path', path);
            $item.find(".path").text(path);
            $item.click({path: path}, _.bind(this.onSelect, this));
            this.setItemState($item);
            return $item;
        },

        setItemState: function ($item) {
            var path = $item.attr('data-path');
            if (path == browser.getCurrentPath()) {
                this.setNodeState($item, browser.current.node);
            } else {
                core.getJson('/bin/cpm/nodes/node.tree.json' + path, _.bind(function (result) {
                    this.setNodeState($item, result);
                }, this), _.bind(function (result) {
                    $item.remove();
                }, this));
            }
        },

        setNodeState: function ($item, node) {
            if (node) {
                $item.find('.name').text(node.name ? node.name : 'jcr:root');
                var treeRule = core.components.treeTypes[node.type];
                if (!treeRule) {
                    treeRule = core.components.treeTypes['default'];
                }
                $item.find('i').addClass(treeRule.icon)
            }
        },

        clearFavorites: function (event) {
            event.preventDefault();
            event.stopPropagation();
            this.$favoritesList.html('');
            this.saveProfile();
        },

        clearHistory: function (event) {
            event.preventDefault();
            event.stopPropagation();
            this.$recentlyList.html('');
            this.saveProfile();
        },

        loadProfile: function () {
            this.loadProfileList('favorites', this.$favoritesList);
            this.loadProfileList('recently', this.$recentlyList);
        },

        loadProfileList: function (key, $list) {
            var values = core.console.getProfile().get('favorites', key);
            if (_.isArray(values)) {
                for (var i = 0; i < values.length; i++) {
                    var item = this.createItem(values[i]);
                    if (item) {
                        $list.append(item);
                    }
                }
            }
        },

        saveProfile: function () {
            this.saveProfileList('favorites', this.$favoritesList);
            this.saveProfileList('recently', this.$recentlyList);
        },

        saveProfileList: function (key, $list) {
            var values = [];
            $list.children().each(function () {
                values.push($(this).attr('data-path'));
            });
            core.console.getProfile().set('favorites', key, values);
        }
    });

    browser.favorites = core.getView('#favorites-view', browser.Favorites);

})(window.core.browser);
