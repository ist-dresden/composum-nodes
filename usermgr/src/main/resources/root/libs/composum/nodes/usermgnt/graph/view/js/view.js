(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, graph, core) {

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
                graph.render(undefined, usermanagement.current.node.name, undefined, 'view', _.bind(function () {
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
