(function () {
    'use strict';
    CPM.namespace('nodes.aem.assets');
    (function (assets, browser, core) {

        assets.ImageView = browser.AbstractDisplayTab.extend({

            initialize: function (options) {
                options = _.extend(options, {
                    displayKey: 'aemAssetView',
                    loadContent: _.bind(function (url) {
                        this.$image.attr('src', '');
                        this.$image.attr('src', core.getContextUrl(url));
                    }, this)
                });
                this.$image = this.$('.image-frame img');
                browser.AbstractDisplayTab.prototype.initialize.call(this, options);
            }
        });

        browser.registerGenericTab('aem-asset', assets.ImageView);

    })(CPM.nodes.aem.assets, CPM.nodes.browser, CPM.core);
})();
