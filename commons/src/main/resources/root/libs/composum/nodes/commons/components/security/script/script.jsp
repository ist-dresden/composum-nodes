<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="scriptModel" scope="request">
    <div class="composum-nodes-security-config_script">
        <div class="script-title title">${cpn:text(scriptModel.script.label)}
            <cpn:text
                    tagName="span" class="rank" format="({})" value="${scriptModel.script.rank}"/>
            <div class="category">[${cpn:text(scriptModel.script.categories)}]</div>
        </div>
        <cpn:text class="script-description description">${scriptModel.script.description}</cpn:text>
        <a class="script-path path" href="${scriptModel.scriptUrl}" target="_top">${scriptModel.script.scriptPath}</a>
    </div>
</cpn:component>
