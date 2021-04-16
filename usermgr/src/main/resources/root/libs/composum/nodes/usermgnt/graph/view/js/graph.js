window.CPM = window.CPM || {};
window.CPM.nodes = window.CPM.nodes || {};
window.CPM.nodes.usermgr = window.CPM.nodes.usermgr || {};
window.CPM.nodes.usermgr.graph = window.CPM.nodes.usermgr.graph || {};

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

    graph.render = function (type, name, path, selector, callback) {
        let params = '';
        if (type) {
            params += '&type=' + encodeURIComponent(type);
        }
        if (name) {
            params += '&name=' + encodeURIComponent(name);
        }
        if (path) {
            params += '&path=' + encodeURIComponent(path);
        }
        params = params.replace(/^&/, '?');
        $.ajax({
            url: '/bin/cpm/users/graph.graphviz' + (selector ? '.' + selector : '') + '.html' + params,
            cache: false
        }).done(function (dot) {
            const $graph = $('.composum-nodes-usermgr-graph');
            const graphviz = d3.select($graph[0]).graphviz();
            graphviz.on('end', function () {
                const $svg = $graph.find('svg');
                const $img = $('.composum-nodes-usermgr-graph_image img');
                if ($svg.length > 0 && $img.length > 0) {
                    try {
                        $img.attr('src', graph.svg2img($svg[0]));
                    } catch (ex) {
                        if (console) console.log(ex);
                    }
                }
            });
            graphviz.renderDot(dot, callback);
        });
    };

})(window.CPM.nodes.usermgr.graph);
