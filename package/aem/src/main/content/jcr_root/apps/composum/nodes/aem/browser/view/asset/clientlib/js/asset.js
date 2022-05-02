(function () {
    'use strict';
    CPM.namespace('nodes.aem.assets');
    (function (assets, browser, core) {

        assets.AssetView = browser.AbstractDisplayTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'aemAssetView',
                    loadContent: _.bind(function (url) {
                        switch (this.type) {
                            case 'image':
                                this.$image.attr('src', '');
                                this.$image.attr('src', core.getContextUrl(url));
                                break;
                            case 'video':
                                this.$video.attr('src', '');
                                this.$video.attr('src', core.getContextUrl(url));
                                break;
                            case 'file':
                                this.$frame.attr('src', core.getContextUrl('/bin/cpm/nodes/node.download.bin' + url));
                                break;
                            default:
                                break;
                        }
                    }, this)
                });
                this.type = this.$el.data('type') || '';
                switch (this.type) {
                    case 'image':
                        this.$image = this.$('.image-frame img');
                        break;
                    case 'video':
                        this.$video = this.$('.video-frame video source');
                        break;
                    case 'file':
                        this.$frame = this.$('.file-frame');
                        break;
                    default:
                        break;
                }
                browser.AbstractDisplayTab.prototype.initialize.call(this, options);
            }
        });

        browser.registerGenericTab('aem-asset', assets.AssetView);

    })(CPM.nodes.aem.assets, CPM.nodes.browser, CPM.core);
})();
