<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.security.SetupConfiguration">
    <div class="composum-nodes-security-config_scripts-list">
        <c:forEach items="${model.matchingSet}" var="configBean">
            <dl class="composum-nodes-security-config_config-item">
                <dt class="composum-nodes-security-config_config-head">
                    <c:set var="configModel" value="${configBean}" scope="request"/>
                    <sling:include path="${configBean.config.path}"
                                   resourceType="composum/nodes/commons/components/security/config"/>
                    <c:remove var="configModel" scope="request"/>
                </dt>
                <c:forEach items="${configBean.scripts}" var="scriptBean">
                    <dd class="composum-nodes-security-config_script-item">
                        <input type="checkbox" name="script" value="${scriptBean.script.scriptPath}"
                               class="composum-nodes-security-config_script-select"/>
                        <c:set var="scriptModel" value="${scriptBean}" scope="request"/>
                        <sling:include path="${scriptBean.script.path}"
                                       resourceType="composum/nodes/commons/components/security/script"/>
                        <c:remove var="scriptModel" scope="request"/>
                    </dd>
                </c:forEach>
            </dl>
        </c:forEach>
    </div>
</cpn:component>
