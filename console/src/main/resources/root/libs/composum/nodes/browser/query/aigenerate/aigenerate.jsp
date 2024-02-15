<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>

<div class="aigenerate-popover">
    <form class="form-horizontal">
        <div class="form-group row">
            <div class="col-sm-2">
                <label for="aigenerateQuery" class="col-form-label">Describe your query</label>
                <div>
                    <button type="submit" id="aigenerateQuerySubmit" class="btn btn-primary">Generate</button>
                </div>
            </div>
            <div class="col-sm-10">
                <textarea class="form-control" id="aigenerateQuery" name="inputText" rows="3"></textarea>
            </div>
        </div>
    </form>

    <table class="table template-links">
        <tr>
            <td></td>
            <td width="100%" id="aigenerateComment" data-type="comment"></td>
        </tr>
        <tr>
            <td>XPath</td>
            <td width="100%"><a href="#" id="aigenerateXpath" data-type="xpath"></a></td>
        </tr>
        <tr>
            <td>SQL2</td>
            <td width="100%"><a href="#" id="aigenerateSql2" data-type="sql2"></a></td>
        </tr>
    </table>
</div>
