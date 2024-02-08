/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.browser.caconfig');

    (function (caconfig, browser, core) {

        caconfig.getTab = function () {
            return core.getView('.node-view-panel .caconfig', caconfig.CaconfigTab);
        };

        caconfig.CaconfigTab = core.console.DetailTab.extend({

            initialize: function (options) {
                this.$toolbar = this.$('.caconfig-toolbar');
                this.$content = this.$('.detail-content');
                this.$toolbar.find('.add').click(_.bind(this.createConfigurationResource, this));
                this.$toolbar.find('.remove').click(_.bind(this.deleteConfigurationResource, this));
                this.$content.on('click', '.create-configuration-resource', _.bind(this.createConfigurationResource, this));
                this.$content.on('click', '.create-configuration-collection-resource', _.bind(this.createConfigurationResource, this));
                this.$content.on('click', '.caconfig-property-editor', _.bind(this.openPropertyEditDialog, this));
                this.$content.on('click', '.target-link[data-path]', this.jumpToTarget.bind(this));
            },

            createConfigurationResource: function (event) {
                var $button = $(event.currentTarget);
                var path = $button.data('path');
                var name = $button.data('nodename');
                var type = $button.data('type');

                var dialog = core.nodes.getCreateNodeDialog();
                dialog.show(_.bind(function () {
                    if (path) {
                        dialog.initParentPath(path);
                    }
                    if (name) {
                        dialog.initName(name);
                    }
                    if (type) {
                        dialog.initType(type);
                    }
                }, this));
            },

            deleteConfigurationResource: function (event) {
                var selectedConfiguration = this.$content.find('.selected-configuration:checked');
                var path = selectedConfiguration.data('path');
                var name = selectedConfiguration.data('name');
                if (!path || !name) {
                    return;
                }
                var dialog = core.nodes.getDeleteNodeDialog();
                dialog.show(_.bind(function () {
                    dialog.setPath(path + '/' + name);
                    dialog.setSmart(false);
                }, this));
            },

            initContent: function () {
                this.$content.find('[data-toggle="tooltip"]').tooltip();
            },

            reload: function () {
                this.$el.addClass('loading');
                core.ajaxGet(core.getComposumPath('composum/nodes/browser/components/caconfig.content.html') +
                    core.encodePath(browser.getCurrentPath()),
                    {},
                    _.bind(function (content) {
                        this.$content.html(content);
                        this.initContent();
                    }, this), undefined, _.bind(function () {
                        this.$el.removeClass('loading');
                    }, this));
            },

            openPropertyEditDialog: function (event) {
                console.log('openPropertyEditDialog', arguments);
                var $target = $(event.currentTarget);
                var path = $target.data('path');
                var propertyName = $target.data('propertyname');
                var config = {
                    path: path,
                    name: propertyName,
                    type: $target.data('typename'),
                    value: $target.data('value'),
                    multi: $target.data('multi'),
                    description: $target.data('description'),
                    required: $target.data('required'),
                    properties: $target.data('properties')
                };
                core.openLoadedDialog(core.getComposumPath('composum/nodes/browser/components/caconfig/property.dialog.html') +
                    core.encodePath(path) + "?propertyName=" + propertyName,
                    caconfig.PropertyEdit, config, function (dialog) {
                        // init dialog
                    }.bind(this), function (dialog) {
                        // value was changed
                        this.reload();
                    }.bind(this)
                );
            },

            jumpToTarget: function (event) {
                event.preventDefault();
                browser.setCurrentPath($(event.currentTarget).data('path'));
                return false;
            },

        });

        caconfig.PropertyEdit = browser.PropertyDialog.extend({

            initialize: function (options) {
                browser.PropertyDialog.prototype.initialize.call(this, options);
                this.$type = this.$el.find('input[name="type"]');
                var oldtitle = this.$title.html(); // will be overwritten
                this.setProperty(new Map(Object.entries(options)));
                this.$title.html(oldtitle);
                this.$el.find('.description').text(options.description);
                this.$el.find('[data-toggle="tooltip"]').tooltip();
                this.$el.find('input[name="required"]').prop('checked', options.required ? 'checked' : '');
                this.$el.find('input[name="multicheckbox"]').prop('checked', options.multi ? 'checked' : '');
                this.$el.find('input[name="multi"]').val(options.multi);
                var properties = options.properties;
                this.displayProperties(properties);
            },

            /** Creates an appropriate display of properties. Examples for propertiesJson:
             * - {pathbrowserRootPathContext=true, widgetType=pathbrowser}
             * - { "dropdownOptions": "[{'value':'option1','description':'First+option'},{'value':'option2','description':'Second+option'},{'value':'option3','description':'Third+option'}]", "widgetType": "dropdown" }
             * We employ some heuristics to display the properties in a human-readable way, and fall back to JSON if we can't parse it.
             * */
            displayProperties: function (propertiesEncoded) {
                var $propertiesContainer = this.$el.find('.propertiesContainer').addClass('hidden');
                if (propertiesEncoded) {
                    var properties = JSON.parse(decodeURIComponent(propertiesEncoded));
                    var $propertiesContent = this.$el.find('.propertiesContent').empty();
                    var $propertiesList = $('<ul></ul>').appendTo($propertiesContent);
                    for (var key in properties) {
                        if (properties.hasOwnProperty(key)) {
                            // display as list of key-value pairs
                            var $property = $('<li></li>').appendTo($propertiesList);
                            $property.append($('<span class="key"></span>').text(key));
                            $property.append($('<span class="separator"></span>').text(': '));
                            var $value = $('<span class="value"></span>').appendTo($property);
                            // if the value is a JSON with an array display it as a list, else as text.
                            var value = properties[key];
                            // try to parse the value as JSON
                            try {
                                value = JSON.parse(value);
                            } catch (e) {
                                try { // weird format for AEM options
                                    value = JSON.parse(value.replaceAll("'", '"'));
                                } catch (e2) {
                                    // ignore
                                }
                            }
                            if (Array.isArray(value)) {
                                var $valueList = $('<ul></ul>').appendTo($value);
                                for (var i = 0; i < value.length; i++) {
                                    var $valueListItem = $('<li></li>').appendTo($valueList);
                                    // if it's {'value':'option1','description':'First+option'}
                                    // display as 'option1: First option'
                                    // otherwise as JSON
                                    if (value[i].value && value[i].description && Object.keys(value[i]).length === 2) {
                                        $valueListItem.text(value[i].value + ': ' + value[i].description.replaceAll('+', ' '));
                                    } else {
                                        $valueListItem.text(JSON.stringify(value[i]));
                                    }
                                }
                            } else {
                                $value.text(value);
                            }
                        }
                    }
                    $propertiesContainer.removeClass('hidden');
                }
            }
        });

    })(CPM.nodes.browser.caconfig, CPM.nodes.browser, CPM.core);

})();
