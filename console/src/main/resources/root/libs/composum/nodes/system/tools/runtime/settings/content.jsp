<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<sling:defineObjects/>
<div class="content-wrapper">
    <div class="tools-runtime-settings_header">
        <h1>${cpn:i18n(slingRequest,'Settings')}</h1>
        <div class="tools-runtime-settings_tabs">
            <a href="#" class="settings_tab" data-selector="sling">Sling</a>
            <a href="#" class="settings_tab" data-selector="http">HTTP</a>
            <a href="#" class="settings_tab" data-selector="system">System</a>
        </div>
    </div>
    <table class="tools-runtime-settings_table table">
    </table>
</div>
