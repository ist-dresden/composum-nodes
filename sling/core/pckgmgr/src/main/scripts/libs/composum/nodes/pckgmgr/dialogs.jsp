<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="dialogs/pckg-create.jsp"/>
<sling:call script="dialogs/pckg-delete.jsp"/>
<sling:call script="dialogs/pckg-upload.jsp"/>
<sling:call script="jcrpckg/general/pckg-update.jsp"/>
<sling:call script="jcrpckg/filter/filter-change.jsp"/>
<sling:call script="jcrpckg/options/change-options.jsp"/>
<sling:call script="jcrpckg/options/change-relations.jsp"/>
<sling:include resourceType="/libs/composum/nodes/console/dialogs" replaceSelectors="minimal"/>
<sling:call script="/libs/composum/nodes/console/dialogs/purge-audit.jsp"/>
