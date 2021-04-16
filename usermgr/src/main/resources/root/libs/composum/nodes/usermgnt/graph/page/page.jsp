<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<html>
<head>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1, user-scalable=no"/>
    <link rel="stylesheet" href="/libs/composum/nodes/usermgnt/graph/view/css/graph.css">
    <link rel="stylesheet" href="/libs/composum/nodes/usermgnt/graph/page/css/graph.css">
</head>
<body class="composum-nodes-usermgr-graph_body">
<h3>Authorizables Graph</h3>
<%
    String type = slingRequest.getParameter("type");
    String name = slingRequest.getParameter("name");
    String path = slingRequest.getParameter("path");
%>
<form action="/bin/cpm/users/graph.page.html" method="GET"
      class="composum-nodes-usermgr-graph_page-form">
    <div class="composum-nodes-usermgr-graph_page-form_field form-field_type">
        <label>Type</label>
        <select name="type">
            <option value="" <%=type == null ? "selected" : ""%>>all</option>
            <option <%="user".equals(type) ? "selected" : ""%>>user</option>
            <option <%="group".equals(type) ? "selected" : ""%>>group</option>
        </select>
    </div>
    <div class="composum-nodes-usermgr-graph_page-form_field form-field_name">
        <label title="the authorizable name or a query pattern (%) for filtering by name">Name</label>
        <input name="name" type="text" value="<%= name != null ? name : ""%>"/>
    </div>
    <div class="composum-nodes-usermgr-graph_page-form_field form-field_path">
        <label title="a regular expression for filtering by authorizable path (find)">Path</label>
        <input name="path" type="text" value="<%= path != null ? path : ""%>"/>
    </div>
    <button type="submit">Submit</button>
    <button type="button" class="composum-nodes-usermgr-graph_show-image">Show Image ...</button>
</form>
<sling:include resourceType="composum/nodes/usermgnt/graph/view"/>
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://d3js.org/d3.v5.min.js"></script>
<script src="https://unpkg.com/@hpcc-js/wasm@0.3.11/dist/index.min.js"></script>
<script src="https://unpkg.com/d3-graphviz@3.0.5/build/d3-graphviz.min.js"></script>
<script src="/libs/composum/nodes/usermgnt/graph/view/js/graph.js"></script>
<script>
    $(document).ready(function () {
        window.CPM.nodes.usermgr.graph.render(
            <%= type != null ? "'" + type + "'" : "''"%>,
            <%= name != null ? "'" + name + "'" : "''"%>,
            <%= path != null ? "'" + path + "'" : "''"%>,
            "page"
        );
        $('.composum-nodes-usermgr-graph_show-image').click(window.CPM.nodes.usermgr.graph.showSvgImage);
    });
</script>
</body>
</html>
