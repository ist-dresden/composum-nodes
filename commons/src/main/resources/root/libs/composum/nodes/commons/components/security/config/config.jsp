<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="configModel" scope="request">
    <div class="composum-nodes-security-config_config">
        <h4 class="config-title title">${cpn:text(configModel.config.label)}<cpn:text
                tagName="span" class="rank" format="({})" value="${configModel.config.rank}"/>
            <div class="category">[${cpn:text(configModel.config.categories)}]</div></h4>
        <cpn:text class="config-description description">${configModel.config.description}</cpn:text>
    </div>
</cpn:component>
