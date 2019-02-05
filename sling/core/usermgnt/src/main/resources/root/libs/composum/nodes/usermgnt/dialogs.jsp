<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:include resourceType="/libs/composum/nodes/console/dialogs" replaceSelectors="minimal"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/disableuser/user-disable.jsp"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/changepassword/user-changepw.jsp"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/adduser/user-create.jsp"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/addsystemuser/systemuser-create.jsp"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/addgroup/group-create.jsp"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/addtogroup/add-to-group.jsp"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/addmember/add-member.jsp"/>
<sling:call script="/libs/composum/nodes/usermgnt/dialogs/deleteauthorizable/authorizable-delete.jsp"/>
