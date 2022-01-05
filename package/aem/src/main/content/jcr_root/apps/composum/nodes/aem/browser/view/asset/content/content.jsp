<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component id="genericView" type="com.composum.sling.nodes.browser.GenericView" scope="request">
    <div class="image-frame ${genericView.mimeTypeCss}">
        <div class="image-background"
             style="background-image:url(${cpn:unmappedUrl(slingRequest,cpn:cpm('composum/nodes/commons/images/image-background.png'))})">
            <img src="" alt=""/>
        </div>
    </div>
</cpn:component>
