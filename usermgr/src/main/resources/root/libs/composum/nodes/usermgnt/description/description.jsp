<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="description">
    <div class="row">
        <div class="left col col-xs-6">
            <sling:call script="short.jsp"/>
        </div>
        <div class="left col col-xs-6">
            <ul>
                <li>create and manage groups, system users and regular user accounts in Apache Jackrabbit repositories</li>
                <li>define user groups and assign users to groups</li>
                <li>view and edit properties of groups and users</li>
                <li>view user permissions to specific content nodes</li>
                <li>allow administrators to reset user passwords and lock/unlock or delete user accounts</li>
            </ul>
        </div>
    </div>
    <ul class="nav nav-tabs">
        <li class="active"><a data-toggle="pill" href="#usermanager-user">User Browser</a></li>
        <li><a data-toggle="pill" href="#usermanager-user-rights">User Rights</a></li>
        <li><a data-toggle="pill" href="#usermanager-group">Group View</a></li>
        <li><a data-toggle="pill" href="#usermanager-relationship-graph">Relationship Graph</a></li>
        <li><a data-toggle="pill" href="#usermanager-systemusers">System Users</a></li>
    </ul>
    <div class="tab-content">
        <div id="usermanager-user" class="tab-pane fade active in">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/usermgnt/description/images/usermanager-user.png'))}"
                 alt="User Manager User Browser View incl. Query"/>
        </div>
        <div id="usermanager-user-rights" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/usermgnt/description/images/usermanager-user-rights.png'))}"
                 alt="User Manager Users Rights View"/>
        </div>
        <div id="usermanager-group" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/usermgnt/description/images/usermanager-group.png'))}"
                 alt="User Manager Group View"/>
        </div>
        <div id="usermanager-relationship-graph" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/usermgnt/description/images/usermanager-relationship-graph.png'))}"
                 alt="User Manager Relationship Graph View"/>
        </div>
        <div id="usermanager-systemusers" class="tab-pane fade">
            <img class="img-responsive"
                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/usermgnt/description/images/usermanager-systemusers.png'))}"
                 alt="User Manager System User View"/>
        </div>
    </div>
</div>
