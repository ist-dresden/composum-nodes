<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<div class="detail-panel packages">
    <div class="display-toolbar detail-toolbar">
        <sling:include resourceType="composum/nodes/pckgmgr" replaceSelectors="helpbutton"/>
    </div>

    <div class="container vertically-centered">
        <div class="packages-detail panel panel-default">
            <div class="panel-heading">
                <h3 class="panel-title">Package Manager</h3>
            </div>
            <div class="panel-body">
                <p>
                    The <a href="https://www.composum.com/home/nodes/pckgmgr.html">Package Manager</a>
                    provides a set of tools to manage the content of a Sling instance using
                    <a href="http://jackrabbit.apache.org/filevault/index.html">Apache Jackrabbit FileVault</a> content
                    packages.
                    It is possible to create and install packages, to manage the content of the package
                    repository and to manage the content of the package filter.
                </p>
                <p>
                    The package manager provides two modi, switchable in the "Manager" and "Registry" tabs of the
                    package tree. Both provide the functionality to inspect, up-/download, install and uninstall
                    packages.
                    The "Manager" modus is concerned only with packages stored in the JCR repository itself,
                    while the "Registry" modus manages packages in
                    <a href="https://jackrabbit.apache.org/filevault/apidocs/org/apache/jackrabbit/vault/packaging/registry/PackageRegistry.html">PackageRegistries</a>
                    which support JCR and file system storage, but it currently cannot provide package creation and
                    editing via the UI.
                </p>
                <p>
                    <c:choose>
                        <c:when test="session[Packages]">
                            The current package registry is configured to use the JCR repository.
                        </c:when>
                    </c:choose>
                </p>
            </div>
        </div>
    </div>

</div>
