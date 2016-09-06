/**
 *
 *
 */
(function(core) {
    'use strict';

    core.components = core.components || {};

(function(components) {

    /**
     * Set up all components of a DOM element; add a view at each element marked with
     * a component css class. Uses the 'core.getView()' function to add the View to the
     * DOM element itself to avoid multiple View instances for one DOM element.
     */
    components.setUp = function(root) {
        var $root = $(root);
        $root.find('.widget.checkbox-widget').each(function() {
            core.getView(this, components.CheckboxWidget);
        });
        $root.find('.widget.radio-group-widget').each(function() {
            core.getView(this, components.RadioGroupWidget);
        });
        $root.find('.widget.combo-box-widget').each(function() {
            core.getView(this, components.ComboBoxWidget);
        });
        $root.find('.widget.text-field-widget').each(function() {
            core.getView(this, components.TextFieldWidget);
        });
        $root.find('.widget.text-area-widget').each(function() {
            core.getView(this, components.TextAreaWidget);
        });
        $root.find('.widget.path-widget').each(function() {
            core.getView(this, components.PathWidget);
        });
        $root.find('.widget.number-field-widget').each(function() {
            core.getView(this, components.NumberFieldWidget);
        });
        $root.find('.widget.date-time-widget').each(function() {
            core.getView(this, components.DateTimeWidget);
        });
        $root.find('.widget.file-upload-widget').each(function() {
            core.getView(this, components.FileUploadWidget);
        });
        $root.find('.widget.property-name-widget').each(function() {
            core.getView(this, components.PropertyNameWidget);
        });
        $root.find('.widget.repository-name-widget').each(function() {
            core.getView(this, components.RepositoryNameWidget);
        });
        $root.find('.widget.primary-type-widget').each(function() {
            core.getView(this, components.PrimaryTypeWidget);
        });
        $root.find('.widget.mixin-type-widget').each(function() {
            core.getView(this, components.MixinTypeWidget);
        });
        $root.find('.widget.reference-widget').each(function() {
            core.getView(this, components.ReferenceWidget);
        });
        $root.find('.widget.code-editor-widget').each(function() {
            core.getView(this, components.CodeEditorWidget);
        });
        $root.find('.widget.multi-form-widget').each(function() {
            core.getView(this, components.MultiFormWidget);
        });
        $root.find('form.widget-form').each(function() {
            core.getView(this, components.FormWidget);
        });
    };

})(core.components);

})(window.core);
