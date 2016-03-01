<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="/libs/composum/sling/console/page/dialogs.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/user-disable.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/user-changepw.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/user-create.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/systemuser-create.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/group-create.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/add-to-group.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/add-member.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/authorizable-delete.jsp"/>
