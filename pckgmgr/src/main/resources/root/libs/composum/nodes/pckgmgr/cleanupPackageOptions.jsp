<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<sling:include resourceType="composum/nodes/pckgmgr/group" replaceSelectors="cleanupSelection" />
<!-- http://localhost:9090/bin/packages.cleanupPackageOptions.html/@jcr/hpsgroup/hpsx/1.4
http://localhost:9090/bin/packages.cleanupPackageOptions.html/@fs/hpsgroup/hpsx
http://localhost:9090/bin/packages.cleanupPackageOptions.html/hpsgroup/hpsx
-->
