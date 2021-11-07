(function () {
    'use strict';
    CPM.namespace('console');

    (function (console, core) {

        console.ToolsMenu = Backbone.View.extend({

            el: $('header.navbar .nav .nav-item.tools')[0],

            initialize: function () {
                var $body = $('body');
                var data = $body.data('tools');
                if (data) {
                    this.tools = JSON.parse(data ? atob(data) : '{}');
                    this.currentId = $body.attr('id');
                    this.data = this.getData(this.currentId);
                    var redirectId = core.console.getProfile().get(this.currentId, 'current', '');
                    if (redirectId && this.currentId !== redirectId) {
                        var redirect = this.tools[redirectId];
                        if (redirect && redirect.url) {
                            window.location.href = redirect.url;
                        }
                    }
                    var cfg = this.data;
                    while (cfg && cfg.parent) {
                        core.console.getProfile().set(cfg.parent, 'current', cfg.id);
                        cfg = this.getData(cfg.parent);
                    }
                    $('header.navbar .nav .nav-item.tools').addClass('active');
                }
            },

            getData: function (id) {
                if (id === 'tools') {
                    return undefined;
                }
                var result = {
                    id: id,
                    cfg: this.tools[id]
                }
                result.parent = result.cfg && result.cfg.parent ? result.cfg.parent : undefined;
                return result;
            }
        });

        console.toolsMenu = core.getView('header.navbar .nav .nav-item.tools', console.ToolsMenu);

    })(CPM.console, CPM.core);
})();
