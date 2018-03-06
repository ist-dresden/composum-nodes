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
                    form: 'form.form-widget',
                    group: '.form-group',
                    label: 'label .label-text'
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

            retrieveLabel: function () {
                var c = widgets.const.css.selector;
                var $label = this.$el.closest(c.group).find(c.label);
                return $label.length === 1 ? $label.text().trim() : this.retrieveName();
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

            /**
             * validates the current value using the 'rules' and the 'pattern' if present
             */
            validate: function (alertMethod) {
                this.valid = true;
                // check only if this field has a 'name' (included in a form) and is visible
                // prevent from validation check if the 'name' is removed or the class contains 'hidden'
                if (!this.$el.hasClass('hidden') && this.retrieveName()) {
                    var value = this.getValue();
                    if (this.rules) {
                        if (this.valid && this.rules.pattern) {
                            // check pattern only if not blank (blank is valid if allowed explicitly)
                            var valid = this.valid = (this.rules.blank && (!value || value.trim().length < 1))
                                || this.rules.pattern.test(value);
                            if (!valid) {
                                this.alert(alertMethod, 'danger', '',
                                    this.rules.patternHint || "value doesn't match pattern", this.rules.pattern);
                            }
                        }
                        if (this.valid && this.rules.mandatory) {
                            // check for a defined and not blank value
                            var valid = this.valid = (value !== undefined &&
                                (this.rules.blank || value.trim().length > 0));
                            if (!valid) {
                                this.alert(alertMethod, 'danger', '', 'value is mandatory');
                            }
                        }
                    }
                    // the extension hook for further validation in 'subclasses'
                    if (this.valid && _.isFunction(this.extValidate)) {
                        this.valid = this.extValidate(value);
                    }
                    if (this.valid) {
                        this.$el.closest('.form-group').removeClass('has-error');
                    } else {
                        this.$el.closest('.form-group').addClass('has-error');
                    }
                }
                return this.valid;
            },

            initRules: function ($element) {
                if (!$element) {
                    $element = this.$el;
                }
                this.label = $element.data('label') || this.retrieveLabel();
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

        const: {
            url: {
                //                    2:scheme   3:host    5:port     6:path        9:selectors  10:ext     11:suffix  13:params
                sling: new RegExp('^((https?)://([^:/]+)(:([^/]+))?)?(/[^.?]*)(\\.(([^/?]+)\\.)?([^./?]+))?(/[^?]*)?(\\?(.*))?$')
            },
            alert: {
                type: {
                    error: 'danger',
                    warn: 'warning'
                }
            }
        },

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
            var $root = $(root);
            var $element;
            if (typeof element === 'string') {
                if ($root.is(element)) {
                    $element = $root;
                } else {
                    $element = $root.find(element);
                }
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

        getAlertType: function (messageLevel) {
            return core.const.alert.type[messageLevel] || messageLevel;
        },

        /**
         * displays a short 'alert' dialog with a single message
         * @param type the message error level (success, info, warning, danger)
         * @param title the message text to display in the heading of the dialog
         * @param message the message text to display; optional - if not present the alert will hide
         * @param result an optional result object from an Ajax call; a hint from this result is added to the text
         */
        alert: function (type, title, message, result) {
            type = core.getAlertType(type);
            var dialog = core.getView('#alert-dialog', core.components.Dialog);
            dialog.$('.modal-header h4').text(title || 'Alert');
            dialog.show(_.bind(function () {
                dialog.alert(type, message, result);
            }, this));
        },

        /**
         * displays a short 'alert' dialog with a message list
         * @param type the message error level (success, info, warning, danger)
         * @param title the message text to display in the heading of the dialog
         * @param messages the message list with items like {level:(error,warn,info),text:'...'}
         */
        messages: function (type, title, messages) {
            type = core.getAlertType(type);
            var dialog = core.getView('#alert-dialog', core.components.Dialog);
            dialog.$('.modal-header h4').text(title);
            dialog.show(_.bind(function () {
                dialog.messages(type, undefined, messages);
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

        getParentAndName: function (nodePath) {
            var lastSlash = nodePath.lastIndexOf('/');
            if (lastSlash >= 0) {
                return {
                    path: nodePath.substring(0, lastSlash),
                    name: nodePath.substring(lastSlash + 1)
                };
            } else {
                return {
                    path: '',
                    name: nodePath
                };
            }
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

        mangleNameValue: function(name) {
            return name.replace(/[%*!?]+/g, '').replace(/[\s]+/g, '_').replace(/[$%&/#+]+/g, '-');
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

    window.core.SlingUrl = function (url) {
        this.url = url;
        var parts = window.core.const.url.sling.exec(url);
        this.scheme = parts[2];
        this.host = parts[3];
        this.port = parts[5];
        this.context = $('html').data('context-path');
        this.path = parts[6];
        if (this.context) {
            if (this.path.indexOf(this.context) === 0) {
                this.path = this.path.substring(this.context.length);
            }
        }
        this.selectors = parts[9] ? parts[9].split('.') : [];
        this.extension = parts[10];
        this.suffix = parts[11];
        this.parameters = {};
        if (parts[13]) {
            var params = parts[13].split("&");
            for (var i = 0; i < params.length; i++) {
                var pair = params[i].split('=');
                var name = decodeURIComponent(pair[0]);
                if (name) {
                    var value = pair.length > 1 ? decodeURIComponent(pair[1]) : null;
                    if (this.parameters[name]) {
                        if (_.isArray(this.parameters[name])) {
                            this.parameters[name].push(value);
                        } else {
                            this.parameters[name] = [this.parameters[name], value];
                        }
                    } else {
                        this.parameters[name] = value;
                    }
                }
            }
        }
    };

    _.extend(window.core.SlingUrl.prototype, {

        build: function () {
            this.url = "";
            if (this.scheme) {
                this.url += this.scheme + '://';
            }
            if (this.host) {
                if (!this.scheme) {
                    this.url += '//';
                }
                this.url += this.host;
                if (this.port) {
                    this.url += ':' + this.port;
                }
            }
            if (this.context) {
                this.url += this.context;
            }
            if (this.path) {
                this.url += this.path;
            }
            if (_.isArray(this.selectors) && this.selectors.length > 0) {
                this.url += this.selectors.join('.');
            }
            if (this.extension) {
                this.url += '.' + this.extension;
            }
            if (this.suffix) {
                this.url += this.suffix;
            }
            if (this.parameters) {
                var params = '';
                var value, array, object = this.parameters;
                _.each(_.keys(object), function (name) {
                    params += params.length === 0 ? '?' : '&';
                    value = object[name];
                    if (_.isArray(value)) {
                        array = value;
                        for (var i = 0; i < array.length;) {
                            value = array[i];
                            params += encodeURIComponent(name);
                            if (value) {
                                params += '=' + encodeURIComponent(value);
                            }
                            if (++i < array.length) {
                                params += '&';
                            }
                        }
                    } else {
                        params += encodeURIComponent(name);
                        if (value) {
                            params += '=' + encodeURIComponent(value);
                        }
                    }
                });
                this.url += params;
            }
            return this.url;
        }
    });

})(window);
