<%@page session="false" pageEncoding="utf-8"%>
<div class="form-group widget property-value-widget">
  <label class="control-label">Value</label>
  <div class="multi-form-content">
    <div class="multi-form-item">
      <input class="default widget text-field-widget form-control" type="text" />
      <input class="name widget repository-name-widget form-control hidden" type="text" />
      <input class="boolean widget checkbox-widget form-control hidden" type="checkbox" />
      <input class="number widget number-field-widget form-control hidden" type="text" />
      <div class="datetime widget date-time-widget input-group hidden">
        <input type="text" class="form-control" />
        <span class="input-group-addon">
            <span class="glyphicon glyphicon-calendar"></span>
        </span>
      </div>
      <div class="path widget path-widget input-group hidden">
        <input type="text" class="form-control" placeholder="Select path...">
        <span class="input-group-btn">
          <button class="select btn btn-default" type="button" title="Select Repository Path">...</button>
        </span>
      </div>
      <div class="reference widget reference-widget input-group hidden">
        <input type="text" class="form-control" placeholder="Select referenceable...">
        <span class="input-group-btn">
          <button class="select btn btn-default fa fa-link" type="button" title="Select Repository Path"></button>
        </span>
      </div>
      <input class="jcr-primaryType widget primary-type-widget form-control hidden" type="text" />
      <input class="jcr-mixinTypes widget mixin-type-widget form-control hidden" type="text" />
    </div>
  </div>
  <div class="binary widget file-upload-widget hidden">
    <input class="form-control" type="file">
  </div>
</div>
