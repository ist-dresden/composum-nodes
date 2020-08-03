<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="pckg" type="com.composum.sling.core.pckgmgr.view.PackageBean" scope="request">
    <div class="advanced-view">
        <div class="ac-handling select-option option-field">
            <div class="name-line"><i class="option-icon fa fa-key"/><span class="name">AC handling</span><span
                    class="select-value label label-primary"
                    data-value="${pckg.acHandling}">${pckg.acHandlingLabel}</span></div>
        </div>
        <div class="requires-root option-flag option-field">
            <div class="name-line"><i
                    class="flag-icon fa fa<c:if test="${pckg.requiresRoot}">-check</c:if>-square-o"/><span
                    data-value="${pckg.requiresRoot}">requires root</span></div>
        </div>
        <div class="requires-restart option-flag option-field">
            <div class="name-line"><i
                    class="flag-icon fa fa<c:if test="${pckg.requiresRestart}">-check</c:if>-square-o"/><span
                    data-value="${pckg.requiresRestart}">requires restart</span></div>
        </div>
        <div class="provider-name text-option option-field">
            <div class="name-line" class="label">Provider Name</div>
            <div class="value-line value-text">${cpn:text(pckg.providerName)}</div>
        </div>
        <div class="provider-url link-option option-field">
            <div class="name-line">Provider URL</div>
            <div class="value-line"><a href="${cpn:url(slingRequest,pckg.providerUrl)}" class="link value-text">${cpn:text(pckg.providerUrl)}</a></div>
        </div>
        <div class="provider-link link-option option-field">
            <div class="name-line">Provider Link</div>
            <div class="value-line"><a href="${cpn:url(slingRequest,pckg.providerLink)}" class="link value-text">${cpn:text(pckg.providerLink)}</a></div>
        </div>
    </div>
    <div class="relations-view">
        <div class="dependencies">
            <table class="relations-table bootstrap-table table table-striped">
                <thead>
                <tr>
                    <th class="th-inner">Dependencies</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${pckg.dependencies}" var="relation">
                    <tr>
                        <td class="relation">${cpn:text(relation)}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
        <div class="replaces">
            <table class="relations-table bootstrap-table table table-striped">
                <thead>
                <tr>
                    <th class="th-inner">Replaces</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${pckg.replaces}" var="relation">
                    <tr>
                        <td class="relation">${cpn:text(relation)}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</cpn:component>
