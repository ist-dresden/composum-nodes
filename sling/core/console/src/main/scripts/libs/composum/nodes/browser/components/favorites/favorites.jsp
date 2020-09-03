<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.nodes.browser.Browser" scope="request">
    <div id="favorites-view" class="favorites">
        <div class="favorites-panel">
            <div class="marked-nodes scrollable">
                <ol class="list">
                </ol>
            </div>
            <div class="used-recently scrollable">
                <ol class="list">
                </ol>
            </div>
        </div>
        <div class="template">
            <a href="#" data-path="">
                <i class="fa"></i><h4 class="name">Node name</h4>
                <h5 class="path">/content/test/some/node</h5>
            </a>
        </div>
        <div class="action-bar">
            <div class="align-left">
                <div class="btn-group btn-group-sm" role="group">
                    <a href="#" class="clear-favorites fa fa-stack trash"
                            title="Clear favorites list"><span
                            class="fa-star fa-stack-1x text-muted"></span><span
                            class="fa-trash-o fa-stack-1x text-danger"></span>
                    </a>
                    <a href="#" class="clear-recently fa fa-stack trash"
                            title="Clear history"><span
                            class="fa-history fa-stack-1x text-muted"></span><span
                            class="fa-trash-o fa-stack-1x text-danger"></span>
                    </a>
                </div>
            </div>
            <div class="align-right">
                <div class="btn-group btn-group-sm" role="group">
                    <a href="#" class="favorites fa fa-star tab tab-bottom"
                       title="Select favorites list"></a>
                    <a href="#" class="history fa fa-history tab tab-bottom"
                       title="Select history list"></a>
                </div>
                <div class="btn-group btn-group-sm" role="group">
                    <button class="close" title="Close"><span class="fa fa-close"></span></button>
                </div>
            </div>
        </div>
    </div>
</cpn:component>
