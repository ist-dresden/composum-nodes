<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<div class="composum-nodes-security-config">
    <div class="composum-nodes-security-config_top">
        <sling:call script="category.jsp"/>
        <sling:call script="matching.jsp"/>
    </div>
    <div class="composum-nodes-security-config_bottom">
    </div>
</div>
