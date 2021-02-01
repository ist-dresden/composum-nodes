<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <div class="panel-heading header">
        <div class="thumbnail">
            <a class="upload-thumbnail" href="#" title="Upload a new thumbnail image (64x64)">
                <div class="image-wrapper">
                    <img src="${pckg.thumbnailUrl}"/>
                </div>
            </a>
        </div>
        <div class="identifiers">
            <h3>${cpn:text(pckg.group)}&nbsp;</h3>
            <h1>${cpn:text(pckg.name)}</h1>
            <h2>${cpn:text(pckg.version)}&nbsp;</h2>
        </div>
        <div class="description">${cpn:rich(slingRequest,pckg.description)}</div>
    </div>
    <div class="panel-body status">
        <sling:include replaceSelectors="summary"/>
    </div>
</cpn:component>