<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="model" type="com.composum.sling.core.usermanagement.view.User" scope="request">
    <cpn:div class="job property" test="${not empty model.profile.values.jobTitle}">
        <div class="label">${cpn:i18n(slingRequest,'Job Title')}</div>
        <cpn:text class="value">${model.profile.values.jobTitle}</cpn:text>
    </cpn:div>
    <cpn:div class="email property" test="${not empty model.profile.email}">
        <div class="label">${cpn:i18n(slingRequest,'E-Mail')}</div>
        <a href="mailto:${cpn:value(model.profile.email)}" class="value">${cpn:text(model.profile.email)}</a>
    </cpn:div>
    <cpn:div class="phone property" test="${not empty model.profile.phoneNumber}">
        <div class="label">${cpn:i18n(slingRequest,'Phone')}</div>
        <a href="tel:${cpn:value(model.profile.phoneNumber)}" class="value">${cpn:text(model.profile.phoneNumber)}</a>
    </cpn:div>
    <cpn:div class="address property" test="${not empty model.profile.address}">
        <div class="label">${cpn:i18n(slingRequest,'Address')}</div>
        <cpn:text class="value" type="rich">${model.profile.address}</cpn:text>
    </cpn:div>
    <cpn:div class="about property" test="${not empty model.profile.about}">
        <div class="label">${cpn:i18n(slingRequest,'About')}</div>
        <cpn:text class="value" type="rich">${model.profile.about}</cpn:text>
    </cpn:div>
</cpn:component>
