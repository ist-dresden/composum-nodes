<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<a class="${genericElement.key} ${empty genericElement.icon?'':'fa fa-'}${genericElement.icon} btn btn-default ${genericElement.css}"
   href="${genericElement.href}" target="${genericElement.target}"
   title="${genericElement.title}"><span class="label">${genericElement.label}</span></a>
