/**
 *
 *
 */
(function (core) {
    'use strict';

    core.i18n = {

        const: {
            uri: {
                translate: '/bin/cpm/core/translate.object.json'
            }
        },

        _cache: {},

        /**
         * @param value a string or an object / array of string values
         * @param callback optional; enables asynchronous loading with translation as parameter
         * @returns {*} the translation if 'value'
         */
        get: function (value, callback) {
            var missed = core.i18n._missed(value, core.i18n._cache);
            if (!_.isEmpty(missed)) {
                core.ajaxPut(core.i18n.const.uri.translate, JSON.stringify(missed), {
                    dataType: 'json',
                    async: _.isFunction(callback)
                }, _.bind(function (data) {
                    core.i18n._toCache(data, core.i18n._cache);
                    if (_.isFunction(callback)) {
                        callback(core.i18n._fromCache(value, core.i18n._cache));
                    }
                }, this))
            }
            return core.i18n._fromCache(value, core.i18n._cache);
        },

        _missed: function (value, cacheItem) {
            var found = {};
            if (_.isString(value)) {
                var hit = _.isObject(cacheItem) ? cacheItem[value] : cacheItem;
                if (_.isEmpty(hit)) {
                    found[value] = value;
                }
            } else if (_.isArray(value)) {
                value.forEach(function (val) {
                    var hit = cacheItem[val];
                    if (_.isEmpty(hit)) {
                        found[val] = val;
                    }
                })
            } else if (_.isObject(value)) {
                _.keys(value).forEach(function (key) {
                    var hit = cacheItem[key];
                    if (_.isEmpty(hit)) {
                        found[key] = value[key];
                    } else {
                        var missed = core.i18n._missed(value[key], hit);
                        if (!_.isEmpty(missed)) {
                            found[key] = missed;
                        }
                    }
                })
            }
            return found;
        },

        _fromCache: function (value, cacheItem) {
            var result = value;
            if (_.isString(value)) {
                var hit = _.isObject(cacheItem) ? cacheItem[value] : cacheItem;
                if (!_.isEmpty(hit)) {
                    result = hit;
                }
            } else if (_.isArray(value)) {
                result = [];
                value.forEach(function (val) {
                    var hit = core.i18n._fromCache(val, cacheItem);
                    result.push(!_.isEmpty(hit) ? hit : val);
                })
            } else if (_.isObject(value)) {
                result = {};
                _.keys(value).forEach(function (key) {
                    var hit = core.i18n._fromCache(value[key], cacheItem[key]);
                    result[key] = !_.isEmpty(hit) ? hit : value[key];
                })
            }
            return result;
        },

        _toCache: function (data, cacheItem) {
            _.keys(data).forEach(function (key) {
                if (_.isObject(data[key])) {
                    if (!_.isObject(cacheItem[key])) {
                        cacheItem[key] = {};
                    }
                    core.i18n._toCache(data[key], cacheItem[key]);
                } else {
                    cacheItem[key] = data[key];
                }
            })
        }
    };
})
(window.core);
