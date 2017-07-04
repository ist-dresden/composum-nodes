/**
 *
 *
 */
(function (window) {
    'use strict';

    window.widgets = {

        const: {
            css: {
                base: 'widget',
                selector: {
                    general: '.widget',
                    prefix: '.widget.',
                    form: 'form.form-widget'
                }
            },
            attr: {
                name: 'name'
            },
            tag: {
                input: ['input', 'select', 'textarea']
            }
        },

        registered: {},

        /**
         * Register a widget type by a DOM selector as key for this type.
         * @param selector    the jQuery selector to find the widgets instances
         * @param widgetClass the widget implementation class (required)
         * @param options     additional options (see richtext-widget for example)
         */
        register: function (selector, widgetClass, options) {
            widgets.registered[selector] = _.extend({
                selector: selector,
                widgetClass: widgetClass
            }, options);
        },

        /**
         * Set up all components of a DOM element; add a view at each element marked with
         * a widget css class. Uses the 'core.getView()' function to add the View to the
         * DOM element itself to avoid multiple View instances for one DOM element.
         */
        setUp: function (root) {
            var $root = root ? $(root) : $(document);
            _.keys(widgets.registered).forEach(function (selector) {
                $root.find(selector).each(function () {
                    var widget = widgets.registered[selector];
                    core.getView(this, widget.widgetClass);
                });
            });
        },

        /**
         * applies the abstract 'option' (must be a function) to all widgets which have such
         * an option registered and are matching to the widgets selector in the 'root's context
         * used for example
         *  - by the multi-form-widget to apply the 'afterClone' option after item creation (cloning)
         */
        apply: function (root, option) {
            _.keys(widgets.registered).forEach(function (selector) {
                var widget = widgets.registered[selector];
                var method = widget[option];
                if (_.isFunction(method)) {
                    var $root = $(root);
                    if ($root.is(widget.selector)) {
                        method.apply(root);
                    } else {
                        $root.find(widget.selector).each(function () {
                            method.apply(this);
                        });
                    }
                }
            });
        },

        /**
         *
         * #abstract
         */
        Widget: Backbone.View.extend({

            initialize: function (options) {
                this.$input = this.retrieveInput();
                this.name = this.retrieveName();
                this.form = this.retrieveForm();
            },

            retrieveInput: function () {
                var $inputEl = [];
                for (var i = 0; $inputEl.length === 0 && i < widgets.const.tag.input.length; i++) {
                    $inputEl = this.$el.is(widgets.const.tag.input[i]) ? this.$el : this.$(widgets.const.tag.input[i]);
                }
                return $inputEl;
            },

            retrieveName: function () {
                return this.$input.attr(widgets.const.attr.name);
            },

            declareName: function (name) {
                if (name) {
                    this.$input.attr(widgets.const.attr.name, name);
                } else {
                    this.$input.removeAttr(widgets.const.attr.name);
                }
            },

            retrieveForm: function () {
                var $form = this.$el.closest(widgets.const.css.selector.form);
                return $form.length > 0 ? $form[0].view : undefined;
            },

            /**
             * #abstract
             * @returns {undefined}
             */
            getValue: function () {
                return undefined;
            },

            /**
             * #abstract
             */
            setValue: function (value) {
            },

            /**
             * #default
             */
            setDefaultValue: function (value) {
            },

            /**
             * #abstract
             */
            reset: function () {
            },

            /**
             * #abstract
             */
            validate: function (alertMethod) {
            },

            initRules: function ($element) {
                this.label = $element.data('label');
                // scan 'data-pattern' attribute
                var pattern = $element.data('pattern');
                if (pattern) {
                    this.rules = _.extend(this.rules || {}, {
                        pattern: pattern.indexOf('/') === 0
                            // use '/.../ig' to specify pattern and flags
                            ? eval(pattern)
                            // pure strings can not have additional flags...
                            : new RegExp(pattern)
                    });
                    var patternHint = $element.data('pattern-hint');
                    if (patternHint) {
                        this.rules.patternHint = patternHint;
                    }
                }
                // scan 'data-rules' attribute
                var rules = $element.data('rules');
                if (rules) {
                    this.rules = _.extend(this.rules || {}, {
                        mandatory: rules.indexOf('mandatory') >= 0,
                        blank: rules.indexOf('blank') >= 0,
                        unique: rules.indexOf('unique') >= 0
                    });
                }
            },

            /**
             * returns the current validation state, calls 'validate' if no state is present
             */
            isValid: function (alertMethod) {
                if (this.valid === undefined) {
                    this.valid = _.isFunction(this.validate)
                        ? this.validate(alertMethod)
                        : true;
                }
                this.alertFlush(alertMethod);
                return this.valid;
            },

            validationReset: function () {
                this.valid = undefined;
                this.alertMessage = undefined;
            },

            grabFocus: function () {
                this.$input.focus();
            },

            alert: function (alertMethod, type, label, message, hint) {
                if (_.isFunction(alertMethod)) {
                    alertMethod(type, label || this.label, message, hint);
                } else {
                    this.alertMessage = {
                        type: type,
                        label: label,
                        message: message,
                        hint: hint
                    }
                }
            },

            /**
             * print out a probably delayed message
             */
            alertFlush: function (alertMethod) {
                if (_.isFunction(alertMethod) && this.alertMessage) {
                    alertMethod(this.alertMessage.type, this.alertMessage.label || this.label,
                        this.alertMessage.message, this.alertMessage.hint);
                    this.alertMessage = undefined;
                }
            }
        })
    };

    /**
     * register the 'hidden' input as a widget to add the widgets behaviour to such hidden values
     */
    window.widgets.register('.widget.hidden-widget', window.widgets.Widget);

    window.core = {

        getHtml: function (url, onSuccess, onError, onComplete) {
            core.ajaxGet(url, {dataType: 'html'}, onSuccess, onError, onComplete);
        },

        getJson: function (url, onSuccess, onError, onComplete) {
            core.ajaxGet(url, {dataType: 'json'}, onSuccess, onError, onComplete);
        },

        ajaxHead: function (url, config, onSuccess, onError, onComplete) {
            var ajaxConf = _.extend({
                type: 'HEAD',
                url: core.getContextUrl(url)
            }, config);
            core.ajaxCall(ajaxConf, onSuccess, onError, onComplete);
        },

        ajaxGet: function (url, config, onSuccess, onError, onComplete) {
            var ajaxConf = _.extend({
                type: 'GET',
                url: core.getContextUrl(url)
            }, config);
            core.ajaxCall(ajaxConf, onSuccess, onError, onComplete);
        },

        ajaxPost: function (url, data, config, onSuccess, onError, onComplete) {
            var ajaxConf = _.extend({
                type: 'POST',
                url: core.getContextUrl(url),
                data: data
            }, config);
            core.ajaxCall(ajaxConf, onSuccess, onError, onComplete);
        },

        ajaxPut: function (url, data, config, onSuccess, onError, onComplete) {
            var ajaxConf = _.extend({
                type: 'PUT',
                contentType: false,
                url: core.getContextUrl(url),
                data: data
            }, config);
            core.ajaxCall(ajaxConf, onSuccess, onError, onComplete);
        },

        ajaxDelete: function (url, config, onSuccess, onError, onComplete) {
            var ajaxConf = _.extend({
                type: 'DELETE',
                contentType: false,
                url: core.getContextUrl(url)
            }, config);
            core.ajaxCall(ajaxConf, onSuccess, onError, onComplete);
        },

        ajaxCall: function (config, onSuccess, onError, onComplete) {
            var ajaxConf = _.extend({
                async: true,
                cache: false,
                success: function (data, msg, xhr) {
                    if (_.isFunction(onSuccess)) {
                        onSuccess(data, msg, xhr);
                    }
                },
                error: function (result) {
                    if (core.isNotAuthorized(result)) {
                        if (_.isFunction(core.unauthorizedDelegate)) {
                            core.unauthorizedDelegate(function () {
                                // try it once more after delegation to authorize
                                core.ajaxCall(config, onSuccess, onError, onComplete);
                            });
                            return;
                        }
                    }
                    if (result.status < 200 || result.status > 299) {
                        if (_.isFunction(onError)) {
                            onError(result);
                        }
                    } else {
                        if (_.isFunction(onSuccess)) {
                            onSuccess(result);
                        }
                    }
                },
                complete: function (result) {
                    if (core.isNotAuthorized(result)) {
                        if (_.isFunction(core.unauthorizedDelegate)) {
                            return; // prevent from completion concurrent to authorize and retry
                        }
                    }
                    if (_.isFunction(onComplete)) {
                        onComplete(result);
                    }
                }
            }, config);
            $.ajax(ajaxConf);
        },

        ajaxPoll: function (method, url, progress, onSuccess, onError) {
            var xhr = new XMLHttpRequest();
            xhr.onload = function () {
                if (_.isFunction(onSuccess)) {
                    var snippet = xhr.responseText.substring(xhr.previous_text_length);
                    onSuccess(xhr, snippet);
                }
            };
            xhr.onerror = function () {
                if (_.isFunction(onError)) {
                    var snippet = xhr.responseText.substring(xhr.previous_text_length);
                    onError(xhr, snippet);
                }
            };
            xhr.previous_text_length = 0;
            xhr.onreadystatechange = function () {
                if (xhr.readyState > 2) {
                    var snippet = xhr.responseText.substring(xhr.previous_text_length);
                    if (_.isFunction(progress)) {
                        progress(xhr, snippet);
                    }
                    xhr.previous_text_length = xhr.responseText.length;
                }
            };
            xhr.open(method, core.getContextUrl(url), true);
            xhr.send();
        },

        /** deprecated */
        getRequest: function (dataType, url, onSuccess, onError, onComplete) {
            core.ajaxGet(url, {dataType: dataType}, onSuccess, onError, onComplete);
        },

        submitForm: function (formElement, onSuccess, onError, onComplete) {
            var $form = $(formElement);
            if (!$form.is('form')) {
                $form = $form.find('form');
            }
            var action = $form.attr("action");
            var formData = new FormData($form[0]);
            $.ajax({
                type: 'POST',
                url: core.getContextUrl(action),
                data: formData,
                cache: false,
                contentType: false,
                processData: false,
                success: function (result) {
                    if (_.isFunction(onSuccess)) {
                        onSuccess(result);
                    }
                },
                error: function (result) {
                    if (core.isNotAuthorized(result)) {
                        if (_.isFunction(core.unauthorizedDelegate)) {
                            core.unauthorizedDelegate(function () {
                                // try it once more after delegation to authorize
                                core.submitForm(formElement, onSuccess, onError, onComplete);
                            });
                            return;
                        }
                    }
                    if (result.status < 200 || result.status > 299) {
                        if (_.isFunction(onError)) {
                            onError(result);
                        }
                    } else {
                        if (_.isFunction(onSuccess)) {
                            onSuccess(result);
                        }
                    }
                },
                complete: function (result) {
                    if (core.isNotAuthorized(result)) {
                        if (_.isFunction(core.unauthorizedDelegate)) {
                            return; // prevent from completion concurrent to authorize and retry
                        }
                    }
                    if (_.isFunction(onComplete)) {
                        onComplete(result);
                    }
                }
            });
        },

        submitFormPut: function (formElement, data, onSuccess, onError, onComplete) {
            var $form = $(formElement);
            if (!$form.is('form')) {
                $form = $form.find('form');
            }
            var action = $form.attr("action");
            if (!data) {
                data = {};
                var formData = new FormData($form[0]);
                var keys = formData.keys();
                var key;
                while (!(key = keys.next()).done) {
                    var value = formData.getAll(key.value);
                    data[key.value] = value.length === 1 ? value[0] : value;
                }
            }
            $.ajax({
                type: 'PUT',
                url: core.getContextUrl(action),
                data: JSON.stringify(data),
                dataType: 'json',
                cache: false,
                contentType: false,
                processData: false,
                success: function (result) {
                    if (_.isFunction(onSuccess)) {
                        onSuccess(result);
                    }
                },
                error: function (result) {
                    if (core.isNotAuthorized(result)) {
                        if (_.isFunction(core.unauthorizedDelegate)) {
                            core.unauthorizedDelegate(function () {
                                // try it once more after delegation to authorize
                                core.submitFormPut(formElement, data, onSuccess, onError, onComplete);
                            });
                            return;
                        }
                    }
                    if (result.status < 200 || result.status > 299) {
                        if (_.isFunction(onError)) {
                            onError(result);
                        }
                    } else {
                        if (_.isFunction(onSuccess)) {
                            onSuccess(result);
                        }
                    }
                },
                complete: function (result) {
                    if (core.isNotAuthorized(result)) {
                        if (_.isFunction(core.unauthorizedDelegate)) {
                            return; // prevent from completion concurrent to authorize and retry
                        }
                    }
                    if (_.isFunction(onComplete)) {
                        onComplete(result);
                    }
                }
            });
        },

        isNotAuthorized: function (result) {
            return result.status === 401 || result.status === 403;
        },

        resultMessage: function (result, message) {
            var hintPattern = new RegExp('<title>(.+)</title>', 'im');
            var hint = hintPattern.exec(result.responseText);
            var text = (message ? (message + ' - ') : '') + result.status + ': ' + result.statusText
                + (hint ? ('\n\n(' + hint[1] + ')') : '');
            return text;
        },

        getContextUrl: function (url) {
            var contextPath = $('html').data('context-path');
            if (contextPath && url.indexOf(contextPath) !== 0) {
                url = contextPath + url;
            }
            return url;
        },

        /**
         * Retrieves a View bind to a DOM element or creates it; returns the View.
         * @param element the DOM element or a selector to retrieve it
         * @param viewClass the generator function to create the View
         * @param initializer an option initializer callback caller after creation
         * @param force if 'true' the view is (re)created using the 'viewClass' event if a view
         *        is already available; this is useful if a subclass should be used instead of the
         *        general 'components' class which is probably bound during the components 'init'
         */
        getView: function (element, viewClass, initializer, force) {
            return core.getWidget(document, element, viewClass, initializer, force);
        },

        /**
         * Retrieves a View bind to a DOM element or creates it; returns the View.
         * @param root the DOM scope element (the panel or dialog)
         * @param element the DOM element or a selector to retrieve it
         * @param viewClass the generator function to create the View
         * @param initializer an optional initializer callback function called after creation
         *        or an options object for the view construction
         * @param force if 'true' the view is (re)created using the 'viewClass' event if a view
         *        is already available; this is useful if a subclass should be used instead of the
         *        general 'components' class which is probably bound during the components 'init'
         */
        getWidget: function (root, element, viewClass, initializer, force) {
            var $element;
            if (typeof element === 'string') {
                $element = $(root).find(element);
            } else {
                $element = $(element);
            }
            if ($element && $element.length > 0) {
                element = $element[0];
                if (!element.view || force) {
                    var options = {
                        el: element
                    };
                    if (initializer && !_.isFunction(initializer)) {
                        options = _.extend(initializer, options);
                    }
                    element.view = new viewClass(options);
                    if (_.isFunction(initializer)) {
                        initializer.apply(element.view, [element.view]);
                    }
                }
                return element.view;
            }
            return undefined;
        },

        /**
         * returns the widget instance (view) for a DOM element
         */
        widgetOf: function (element, view) {
            if (typeof element === 'string') {
                element = view ? view.$(element) : $(element);
            } else {
                element = $(element);
            }
            var $widget = element.closest('.widget');
            if ($widget && $widget.length > 0) {
                return $widget[0].view;
            }
            return undefined;
        },

        getWidgetName: function (widget) {
            var name;
            if (_.isFunction(widget.getName)) {
                name = widget.getName.apply(widget)
            }
            if (!name && widget.name && widget.name !== 'undefined') {
                name = widget.name;
            }
            if (!name) {
                name = widget.$el.attr('name');
            }
            if (!name) {
                name = widget.$el.data('name');
            }
            if (!name) {
                name = widget.$el.find('[name]').attr('name');
            }
            return name;
        },

        getWidgetNames: function (widget, separator) {
            return core.splitWidgetName(core.getWidgetName(widget));
        },

        splitWidgetName: function (name, separator) {
            if (!separator) {
                separator = name.indexOf('/') >= 0 ? '/' : '.';
            }
            return name.split(separator);
        },

        /**
         * the dialog to select a repository path in a tree view
         */
        alert: function (type, title, message, result) {
            var dialog = core.getView('#alert-dialog', core.components.Dialog);
            dialog.$('.modal-header h4').text(title || 'Alert');
            dialog.show(_.bind(function () {
                dialog.alert(type, message, result);
            }, this));
        },

        //
        // JCR & helper functions for the client layer
        //

        buildContentPath: function (parentPath, nodeName) {
            if (parentPath && parentPath.length > 0) {
                if (parentPath.charAt(parentPath.length - 1) !== '/') {
                    parentPath += '/';
                }
            } else {
                parentPath = '/';
            }
            if (nodeName.indexOf('/') === 0) {
                nodeName = nodeName.substring(1);
            }
            return parentPath + nodeName;
        },

        getNameFromPath: function (nodePath) {
            var lastSlash = nodePath.lastIndexOf('/');
            var name = lastSlash >= 0 ? nodePath.substring(lastSlash + 1) : nodePath;
            return name;
        },

        getParentPath: function (nodePath) {
            var lastSlash = nodePath.lastIndexOf('/');
            var parentPath = nodePath.substring(0, lastSlash > 0 ? lastSlash : lastSlash + 1);
            return parentPath;
        },

        encodePath: function (path) {
            path = encodeURI(path);
            path = path.replace('&', '%26');
            path = path.replace(';', '%3B');
            path = path.replace(':', '%3A');
            path = path.replace('.', '%2E');
            return path;
        },

        // general helpers

        isEmptyObject: function (object) {
            var values = _.values(object);
            for (var i = 0; i < values.length; i++) {
                if (values[i]) {
                    return false;
                }
            }
            return _.isObject(object) || !object;
        },

        addPathSegment: function (path, segment) {
            if (segment) {
                if (path.length > 0 && !path.endsWith('/') && segment.indexOf('/') !== 0) {
                    path += '/';
                }
                path += segment;
                if (!core.endsWith(path, '/')) {
                    path += '/';
                }
            }
            return path;
        },

        endsWith: function (string, snippet) {
            return string.lastIndexOf(snippet) === string.length - snippet.length;
        }
    };

    window.core.LocalProfile = function (key) {
        this.profileKey = key;
        this.aspects = {};
    };

    _.extend(window.core.LocalProfile.prototype, {

        get: function (aspect, key, defaultValue) {
            var object = this.aspects[aspect];
            if (!object) {
                var item = localStorage.getItem(this.profileKey + '.' + aspect);
                if (item) {
                    object = JSON.parse(item);
                    this.aspects[aspect] = object;
                }
            }
            var value = undefined;
            if (object) {
                value = key ? object[key] : object;
            }
            return value !== undefined ? value : defaultValue;
        },

        set: function (aspect, key, value) {
            var object = this.get(aspect, undefined, {});
            if (key) {
                object[key] = value;
            } else {
                object = value;
            }
            this.aspects[aspect] = object;
            this.save(aspect);
        },

        save: function (aspect) {
            var value = this.aspects[aspect];
            if (value) {
                localStorage.setItem(this.profileKey + '.' + aspect, JSON.stringify(value));
            }
        }
    });

})(window);
