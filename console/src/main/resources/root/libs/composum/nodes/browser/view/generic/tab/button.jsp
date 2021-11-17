<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<button type="button"
        class="${genericElement.key} ${empty genericElement.icon?'':'fa fa-'}${genericElement.icon} btn btn-default ${genericElement.css}"
        title="${genericElement.title}"${genericElement.enabled?'':' disabled'}><span class="label">${genericElement.label}</span>
</button>
