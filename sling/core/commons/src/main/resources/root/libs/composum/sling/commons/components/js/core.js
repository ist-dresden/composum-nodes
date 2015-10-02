/**
 *
 *
 */
(function(window) {
    'use strict';

    window.core = {

        getHtml: function(url, onSuccess, onError, onComplete) {
            core.getRequest ('html', url, onSuccess, onError, onComplete);
        },

        getJson: function(url, onSuccess, onError, onComplete) {
            core.getRequest ('json', url, onSuccess, onError, onComplete);
        },

        getRequest: function(dataType, url, onSuccess, onError, onComplete) {
            $.ajax({
                type: 'GET',
                url: url,
                dataType: dataType,
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

        submitForm: function(formElement, onSuccess, onError, onComplete) {
            var $form = $(formElement);
            if (!$form.is('form')) {
                $form = $form.find('form');
            }
            var action = $form.attr("action");
            var formData = new FormData ($form[0]);
            $.ajax({
                type: 'POST',
                url: action,
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

        resultMessage: function(result, message) {
            var hint = result.responseText.match(/^.*<title>(.*)<\/title>.*$/m);
            var text = (message ? (message + ' - ') : '') + result.status + ': ' + result.statusText
                        + (hint ? ('\n\n(' + hint[1] + ')') : '');
            return text;
        },

        /**
         * Retrieves a View bind to a DOM element or creates it; returns the View.
         * @param element the DOM element or a selector to retrieve it
         * @param viewClass the generator function to create the View
         * @param initializer an option initializer callback caller after creation
         */
        getView: function(element, viewClass, initializer) {
            return window.core.getWidget(document, element, viewClass, initializer);
        },

        /**
         * Retrieves a View bind to a DOM element or creates it; returns the View.
         * @param root the DOM scope element (the panel or dialog)
         * @param element the DOM element or a selector to retrieve it
         * @param viewClass the generator function to create the View
         * @param initializer an optional initializer callback function called after creation
         *        or an options object for the view construction
         */
        getWidget: function(root, element, viewClass, initializer) {
            var $element;
            if (typeof element === 'string') {
                $element = $(root).find(element);
            } else {
                $element = $(element);
            }
            if ($element && $element.length > 0) {
                element = $element[0];
                if (!element.view) {
                    var options = {
                        el: element
                    };
                    if (initializer && !_.isFunction(initializer)) {
                        options = _.extend (initializer, options);
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
        alert: function(type, title, message, result) {
            var dialog = core.getView('#alert-dialog', core.components.Dialog);
            dialog.$('.modal-header h4').text(title || 'Alert');
            dialog.show(_.bind (function(){
                dialog.alert(type, message, result);
            }, this));
        },

        //
        // JCR & helper functions for the client layer
        //

        getNameFromPath: function(nodePath) {
            var lastSlash = nodePath.lastIndexOf ('/');
            var name = lastSlash >= 0 ? nodePath.substring (lastSlash + 1) : nodePath;
            return name;
        },

        getParentPath: function(nodePath) {
            var lastSlash = nodePath.lastIndexOf ('/');
            var parentPath = nodePath.substring (0, lastSlash > 0 ? lastSlash : 1);
            return parentPath;
        },

        encodePath: function(path) {
            path = encodeURI(path);
            path = path.replace('&','%26');
            path = path.replace(';','%3B');
            path = path.replace(':','%3A');
            path = path.replace('.','%2E');
            return path;
        }
    };

})(window);
