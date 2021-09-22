<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
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
                    this.$image = this.$('.image-frame img');
                },

                reload: function () {
                    browser.AbstractFileTab.prototype.reload.call(this);
                    this.$download.attr('href', core.getContextUrl(this.$el.data('file')));
                },

                getSelectors: function () {
                    var selectors = this.$selectors.val();
                    return selectors || 'asset';
                }
            });

            browser.registerGenericTab('aem-asset', assets.ImageView);

        })(CPM.nodes.aem.assets, CPM.nodes.browser, CPM.core);
    })();
</script>
