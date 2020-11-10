<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div id="text-edit-dialog" class="dialog modal fade" role="dialog" aria-labelledby="Edit Text"
     aria-hidden="true">
    <div class="text-editor detail-panel">
        <div class="modal-dialog">
            <sling:include resourceType="composum/nodes/console/components/codeeditor"/>
        </div>
    </div>
</div>
