<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<cpn:component var="profile" type="com.composum.sling.core.user.UserProfile" scope="request">
    <cpn:div class="job property" test="${not empty profile.values.jobTitle}">
        <div class="label">${cpn:i18n(slingRequest,'Job Title')}</div>
        <cpn:text class="value">${profile.values.jobTitle}</cpn:text>
    </cpn:div>
    <cpn:div class="email property" test="${not empty profile.email}">
        <div class="label">${cpn:i18n(slingRequest,'E-Mail')}</div>
        <a href="mailto:${cpn:value(profile.email)}" class="value">${cpn:text(profile.email)}</a>
    </cpn:div>
    <cpn:div class="phone property" test="${not empty profile.phoneNumber}">
        <div class="label">${cpn:i18n(slingRequest,'Phone Number')}</div>
        <a href="tel:${cpn:value(profile.phoneNumber)}" class="value">${cpn:text(profile.phoneNumber)}</a>
    </cpn:div>
    <cpn:div class="address property" test="${not empty profile.address}">
        <div class="label">${cpn:i18n(slingRequest,'Address')}</div>
        <cpn:text class="value" type="rich">${profile.address}</cpn:text>
    </cpn:div>
    <cpn:div class="about property" test="${not empty profile.about}">
        <div class="label">${cpn:i18n(slingRequest,'About me')}</div>
        <cpn:text class="value" type="rich">${profile.about}</cpn:text>
    </cpn:div>
</cpn:component>
