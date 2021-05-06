window.CPM = window.CPM || {};
window.CPM.nodes = window.CPM.nodes || {};
window.CPM.nodes.usermgr = window.CPM.nodes.usermgr || {};
window.CPM.nodes.usermgr.graph = window.CPM.nodes.usermgr.graph || {};

/**
 * the 'standalone' implementation of the graph / affected paths feature
 *
 * this can be used as single separated page or as view embedded in the Users console
 * as a single page the container (canvas) for the graph and the paths table is the same
 * in the context of a console view both variations can be placed on the same page; in this
 * case there are two different containers used as canvas element to render the graph and the table
 */
(function (graph) {

    graph.svg2img = function (svg) {
        var xml = new XMLSerializer().serializeToString(svg);
        var svg64 = btoa(xml);
        var b64start = 'data:image/svg+xml;base64,';
        return b64start + svg64;
    };

    graph.showSvgImage = function () {
        const $image = $('.composum-nodes-usermgr-graph_image');
        const $close = $image.find('button');
        $image.addClass('visible');
        $close.click(function () {
            $close.off('click');
            $image.removeClass('visible');
        });
    };

    /**
     * switch between 'graph' and 'paths' mode (in the single page view)
     */
    graph.selectMode = function (event) {
        event.preventDefault();
        const $target = $(event.currentTarget);
        const mode = $target.attr('class');
        localStorage.setItem('composum.nodes.users.graph', JSON.stringify({mode: mode}));
        window.location.reload();
        return false;
    };

    /**
     * renders the graph or the paths table controlled by the local stored profile or the 'selector' parameter
     * @param $element the DOM element wich contains the rendering canvas element
     * @param type the authorizable type filter ('user', 'group' or 'service'; optional)
     * @param name the authorizable name query pattern (uses '%' as wildcard; optional)
     * @param path the authorizable name filter pattern (a regex; supports '%' as wildcard; optional)
     * @param selector the 'page' (single page) or 'view' (console view) selector, optional with an added subselector (e.g. 'view.paths')
     * @param callback an optional callback function in the 'graph' mode to complete the rendered graph
     */
    graph.render = function ($element, type, name, path, text, selector, callback) {
        let query = '';
        if (type) {
            query += '&type=' + encodeURIComponent(type);
        }
        if (name) {
            query += '&name=' + encodeURIComponent(name);
        }
        if (path) {
            query += '&path=' + encodeURIComponent(path);
        }
        if (text) {
            query += '&text=' + encodeURIComponent(text);
        }
        query = query.replace(/^&/, '?');
        graph.execute($element, query, selector, callback);
    };

    graph.execute = function ($element, query, selector, callback) {
        let mode = 'graphviz';
        const parts = /^([^.]+)\.([^.]+)/.exec(selector);
        if (parts) {
            selector = parts[1];
            mode = parts[2]; // mode as subselector of the view selector
        }
        if (selector !== 'view') {
            // in the single 'page' view the current mode is stored locally in the Browser
            const profile = JSON.parse(localStorage.getItem('composum.nodes.users.graph') || '{}');
            mode = profile.mode || 'graphviz';
            $('.composum-nodes-usermgr-graph_body').removeClass('graphviz paths').addClass(mode);
        }
        // load the data according to the mode into the canvas element...
        var url = '/bin/cpm/users/graph.' + mode + (selector ? '.' + selector : '') + '.html' + query;
        $.ajax({
            url: graph.getContextUrl(url),
            cache: false
        }).done(function (content) {
            let $canvas = $element.find('.composum-nodes-usermgr-' + (mode === 'graphviz' ? 'graph' : 'paths'));
            if ($canvas.length < 1) {
                // fallback to the alternative rendering template if preferred template not found
                $canvas = $element.find('.composum-nodes-usermgr-' + (mode === 'graphviz' ? 'paths' : 'graph'));
            }
            switch (mode) {
                default:
                case 'graphviz':
                    $canvas.html('<div class="graphviz-canvas"></div>');
                    const graphviz = d3.select($canvas.find('.graphviz-canvas')[0]).graphviz();
                    graphviz.on('end', function () {
                        // prepare the image directly on the end of the graph rendering
                        // to ensure that the 1:1 zoom is used for image rendering
                        const $svg = $canvas.find('svg');
                        const $img = $element.find('.composum-nodes-usermgr-graph_image img');
                        if ($svg.length > 0 && $img.length > 0) {
                            try {
                                $img.attr('src', graph.svg2img($svg[0]));
                            } catch (ex) {
                                if (console) console.log(ex);
                            }
                        }
                    });
                    graphviz.renderDot(content, callback);
                    break;
                case 'paths':
                    $canvas.html(content);
                    if (callback) {
                        callback();
                    }
                    break;
            }
        });
    };

    graph.getContextUrl = function (url) {
        if (url && !url.match(/^https?:\/\//i)) {  // ignore 'external' URLs
            var contextPath = $('html').data('context-path');
            if (contextPath && url.indexOf(contextPath) !== 0) {
                url = contextPath + url;
            }
        }
        return url;
    }

})(window.CPM.nodes.usermgr.graph);
