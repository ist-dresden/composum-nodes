<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>

<form class="form-horizontal" method="post" action="<%= request.getRequestURI() %>">
    <div class="form-group row">
        <div class="col-sm-2">
            <label for="inputText" class="col-form-label">Describe your query</label>
            <button type="submit" class="btn btn-primary">Generate</button>
        </div>
        <div class="col-sm-10">
            <textarea class="form-control" id="inputText" name="inputText" rows="3"></textarea>
        </div>
    </div>
</form>

<table class="table aigenerate-results">
    <tr>
        <td></td>
        <td width="100%" data-type="comment"></td>
    </tr>
    <tr>
        <td>XPath</td>
        <td width="100%"><a href="#" data-type="xpath"></a></td>
    </tr>
    <tr>
        <td>SQL2</td>
        <td width="100%"><a href="#" data-type="sql2"></a></td>
    </tr>
</table>
