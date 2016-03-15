/**
 *
 *
 */
(function (window) {
    'use strict';

    window.core = {

        getHtml: function (url, onSuccess, onError, onComplete) {
            core.ajaxGet(url, {dataType: 'html'}, onSuccess, onError, onComplete);
        },

        getJson: function (url, onSuccess, onError, onComplete) {
            core.ajaxGet(url, {dataType: 'json'}, onSuccess, onError, onComplete);
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
                url: core.getContextUrl(url),
                data: data
            }, config);
            core.ajaxCall(ajaxConf, onSuccess, onError, onComplete);
        },

        ajaxDelete: function (url, config, onSuccess, onError, onComplete) {
            var ajaxConf = _.extend({
                type: 'DELETE',
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
                    if (_.isFunction(onComplete)) {
                        onComplete(result);
                    }
                }
            }, config);
            $.ajax(ajaxConf);
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
                    if (_.isFunction(onComplete)) {
                        onComplete(result);
                    }
                }
            });
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
            if (contextPath && url.indexOf(contextPath) != 0) {
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
            return window.core.getWidget(document, element, viewClass, initializer, force);
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

        getNameFromPath: function (nodePath) {
            var lastSlash = nodePath.lastIndexOf('/');
            var name = lastSlash >= 0 ? nodePath.substring(lastSlash + 1) : nodePath;
            return name;
        },

        getParentPath: function (nodePath) {
            var lastSlash = nodePath.lastIndexOf('/');
            var parentPath = nodePath.substring(0, lastSlash > 0 ? lastSlash : 1);
            return parentPath;
        },

        encodePath: function (path) {
            path = encodeURI(path);
            path = path.replace('&', '%26');
            path = path.replace(';', '%3B');
            path = path.replace(':', '%3A');
            path = path.replace('.', '%2E');
            return path;
        }
    };

})(window);
