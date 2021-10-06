<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<style>
    .aem-asset .detail-content .image-frame {
        display: table;
        margin: auto;
        border: 1px solid #ccc;
        padding: 5px;
    }

    .aem-asset .detail-content .image-frame .image-background {
        display: inline-block;
    }

    .aem-asset .detail-content .image-frame img {
        width: 100%;
    }

    .aem-asset .detail-content .image-frame.svg img {
        min-width: 100px;
    }
</style>
<script>
    (function () {
        'use strict';
        CPM.namespace('nodes.aem.assets');
        (function (assets, browser, core) {

            assets.ImageView = browser.AbstractFileTab.extend({

                initialize: function (options) {
                    options = _.extend(options, {
                        displayKey: 'imageView',
                        loadContent: _.bind(function (url) {
                            this.$image.attr('src', core.getContextUrl(url));
                        }, this)
                    });
                    browser.AbstractFileTab.prototype.initialize.call(this, options);
                    this.$assetmgr = this.$('.detail-toolbar .assetmgr');
                    this.$image = this.$('.image-frame img');
                },

                reload: function () {
                    browser.AbstractFileTab.prototype.reload.call(this);
                    this.$assetmgr.attr('href', core.getContextUrl('/assetdetails.html' + this.$el.data('file')));
                    this.$download.attr('href', core.getContextUrl(this.$el.data('file')));
                }
            });

            browser.registerGenericTab('aem-asset', assets.ImageView);

        })(CPM.nodes.aem.assets, CPM.nodes.browser, CPM.core);
    })();
</script>
