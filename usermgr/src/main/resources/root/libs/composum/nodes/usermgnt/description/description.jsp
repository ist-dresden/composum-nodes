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
<%-- TODO: add some screenshots --%>
<%--    <ul class="nav nav-tabs">--%>
<%--        <li class="active"><a data-toggle="pill" href="#browser-props">Repository Browser</a></li>--%>
<%--        <li><a data-toggle="pill" href="#browser-view">Render / Image View</a></li>--%>
<%--        <li><a data-toggle="pill" href="#browser-code">Code View and Editing</a></li>--%>
<%--    </ul>--%>
<%--    <div class="tab-content">--%>
<%--        <div id="browser-props" class="tab-pane fade active in">--%>
<%--            <img class="img-responsive"--%>
<%--                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/browser/description/images/browser-props.png'))}"--%>
<%--                 alt="Repository Browser and Query View"/>--%>
<%--        </div>--%>
<%--        <div id="browser-view" class="tab-pane fade">--%>
<%--            <img class="img-responsive"--%>
<%--                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/browser/description/images/browser-view.png'))}"--%>
<%--                 alt="Repository Browser Render View"/>--%>
<%--        </div>--%>
<%--        <div id="browser-code" class="tab-pane fade">--%>
<%--            <img class="img-responsive"--%>
<%--                 src="${cpn:url(slingRequest,cpn:cpm('composum/nodes/browser/description/images/browser-code.png'))}"--%>
<%--                 alt="Repository Browser Code View"/>--%>
<%--        </div>--%>
<%--    </div>--%>
</div>
