<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<button type="button" class="${genericElement.key} fa fa-${genericElement.icon} btn btn-default ${genericElement.css}"
        title="${genericElement.title}"><span class="label">${genericElement.label}</span>
</button>
