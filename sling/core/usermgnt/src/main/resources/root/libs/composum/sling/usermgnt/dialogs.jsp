<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<sling:defineObjects />
<sling:call script="/libs/composum/sling/console/page/dialogs.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/disableuser/user-disable.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/changepassword/user-changepw.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/adduser/user-create.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/addsystemuser/systemuser-create.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/addgroup/group-create.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/addtogroup/add-to-group.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/addmember/add-member.jsp"/>
<sling:call script="/libs/composum/sling/usermgnt/dialogs/deleteauthorizable/authorizable-delete.jsp"/>
