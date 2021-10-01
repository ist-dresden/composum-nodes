<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<a class="${genericElement.key} fa fa-${genericElement.icon} btn btn-default ${genericElement.css}"
   href="${genericElement.href}" target="${genericElement.target}"
   title="${genericElement.title}"><span class="label">${genericElement.label}</span></a>
