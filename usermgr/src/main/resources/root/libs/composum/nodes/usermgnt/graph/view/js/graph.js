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

    graph.selectMode = function (event) {
        event.preventDefault();
        const $target = $(event.currentTarget);
        const mode = $target.attr('class');
        localStorage.setItem('composum.nodes.users.graph', JSON.stringify({mode: mode}));
        window.location.reload();
        return false;
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
        const profile = JSON.parse(localStorage.getItem('composum.nodes.users.graph') || '{}');
        const mode = profile.mode || 'graphviz';
        $('.composum-nodes-usermgr-graph_body').removeClass('graphviz table').addClass(mode);
        $.ajax({
            url: '/bin/cpm/users/graph.' + mode + (selector ? '.' + selector : '') + '.html' + params,
            cache: false
        }).done(function (content) {
            const $graph = $('.composum-nodes-usermgr-graph');
            switch (mode) {
                default:
                case 'graphviz':
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
                    graphviz.renderDot(content, callback);
                    break;
                case'table':
                    $graph.html(content);
                    break;
            }
        });
    };

})(window.CPM.nodes.usermgr.graph);
