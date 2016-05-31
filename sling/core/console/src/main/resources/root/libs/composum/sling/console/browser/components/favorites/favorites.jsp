<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="browser" type="com.composum.sling.core.browser.Browser" scope="request">
    <div id="favorites-view" class="history">
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
    </div>
</cpn:component>
