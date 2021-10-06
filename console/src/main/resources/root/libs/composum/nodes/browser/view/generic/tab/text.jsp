<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<sling:defineObjects/>
<div class="${genericElement.key} input-group input-group-sm">
    <span class="${empty genericElement.icon?'':'fa fa-'}${genericElement.icon} input-group-addon ${genericElement.css}"
          title="${genericElement.title}">${genericElement.label}</span>
    <input type="text" class="form-control" placeholder="${genericElement.placeholder}">
</div>
