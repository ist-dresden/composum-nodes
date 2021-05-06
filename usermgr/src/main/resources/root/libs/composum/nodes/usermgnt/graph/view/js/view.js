(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, graph, core) {

        usermanagement.PathsTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$graph = this.$('.composum-nodes-usermgr-paths');
                this.$filter = this.$('.paths-toolbar .filter');
                this.$reload = this.$('.paths-toolbar .reload');
                this.$filter.val(core.console.getProfile().get('usermgr', 'paths_filter', ''));
                this.$filter.on('change', _.bind(this.reload, this));
                this.$reload.click(_.bind(this.reload, this));
                this.reload();
            },

            reload: function () {
                const text = this.$filter.val();
                graph.render(this.$el, undefined, usermanagement.current.node.name, undefined, text,
                    'view.paths', _.bind(function () {
                        core.console.getProfile().set('usermgr', 'paths_filter', text || '');
                        this.$el.find('.authorizable-id').find('a').click(_.bind(function (event) {
                            const path = $(event.currentTarget).data('path');
                            if (path) {
                                event.preventDefault();
                                usermanagement.setCurrentPath(path);
                                return false;
                            }
                            return true;
                        }, this));
                    }, this));
            }
        });

        usermanagement.GraphTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$graph = this.$('.composum-nodes-usermgr-graph');
                this.$image = this.$('.composum-nodes-usermgr-graph_image');
                this.$toggleImage = this.$('.graph-toolbar .toggle-image');
                this.$openPage = this.$('.graph-toolbar .open-graph-page');
                this.$reload = this.$('.graph-toolbar .reload');
                this.$toggleImage.click(_.bind(this.toggleImageView, this));
                this.$openPage.click(_.bind(this.openGraphPage, this));
                this.$reload.click(_.bind(this.reload, this));
                if (core.console.getProfile().get('usermgr', 'graph', false)) {
                    this.toggleImageView();
                }
                this.reload();
            },

            reload: function (event) {
                graph.render(this.$el, undefined, usermanagement.current.node.name, undefined, undefined,
                    'view', _.bind(function () {
                        this.$graph.find('svg').find('a').click(_.bind(function (event) {
                            const path = $(event.currentTarget).attr('title');
                            if (path) {
                                event.preventDefault();
                                usermanagement.setCurrentPath(path);
                                return false;
                            }
                            return true;
                        }, this));
                    }, this));
            },

            toggleImageView: function () {
                this.$image.toggleClass('visible');
                let visible = this.$image.hasClass('visible');
                core.console.getProfile().set('usermgr', 'graph', visible);
                if (visible) {
                    this.$toggleImage.addClass('active');
                } else {
                    this.$toggleImage.removeClass('active');
                }
            },

            openGraphPage: function () {
                window.open(core.getContextUrl('/bin/cpm/users/graph.page.html?name='
                    + encodeURIComponent(usermanagement.current.node.name)));
            }
        });

    })(CPM.nodes.usermanagement, CPM.namespace('nodes.usermgr.graph'), CPM.core);

})();
