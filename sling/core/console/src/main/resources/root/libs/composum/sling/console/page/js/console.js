/**
 *
 *
 */
(function(core) {
    'use strict';

    core.console = core.console || {};

    core.initPermissions = function() {
        location.reload(); // FIXME: to refactor
    },

(function(console) {

    console.getProfile = function() {
        return console.profile;
    }

    console.getUserLoginDialog = function() {
        return core.getView ('#user-status-dialog', console.UserLoginDialog);
    }

    //
    // login and profile
    //

    console.profile = {

        aspects: {},

        get: function(aspect, key, defaultValue) {
            var object = console.profile.aspects[aspect];
            if (!object) {
                var item = localStorage.getItem('composum.core.' + aspect);
                if (item) {
                    object = JSON.parse(item);
                    console.profile.aspects[aspect] = object;
                }
            }
            var value = undefined;
            if (object) {
                value = key ? object[key] : object;
            }
            return value !== undefined ? value : defaultValue;
        },

        set: function(aspect, key, value) {
            var object = console.profile.get(aspect, undefined, {});
            if (key) {
                object[key] = value;
            } else {
                object = value;
            }
            console.profile.aspects[aspect] = object;
            console.profile.save(aspect);
        },

        save: function(aspect) {
            var value = console.profile.aspects[aspect];
            if (value) {
                localStorage.setItem('composum.core.' + aspect, JSON.stringify(value));
            }
        }
    };

    console.UserLoginDialog = core.components.Dialog.extend({

        initialize: function(options) {
            core.components.Dialog.prototype.initialize.apply(this, [options]);
            var $form = this.$('form');
            var $login = this.$('button.login');
            $form.on('submit',_.bind (this.login, this));
            $login.click(_.bind(this.login, this));
            this.$('button.logout').click(_.bind(this.logout, this));
            this.$el.on('shown.bs.modal', _.bind (this.onShown, this));
        },

        /**
         * initialization after shown...
         */
        onShown: function() {
            this.$('input[name="j_username"]').focus();
        },

        logout: function(event) {
            event.preventDefault();
            core.getHtml('/system/sling/logout.html', undefined, undefined, _.bind (function(data) {
                this.hide();
                core.initPermissions();
            }, this));
        },

        login: function(event) {
            event.preventDefault();
            this.submitForm(undefined, false, _.bind (function() {
                core.initPermissions();
            }, this));
            return false;
        }
    });

    //
    // navbar
    //

    console.NavBar = Backbone.View.extend({

        el: $('header.navbar')[0],

        initialize: function() {
            var consoleId = $('body').attr('id');
            this.$('.nav-item.' + consoleId).addClass('active');
            var loginDialog = console.getUserLoginDialog();
            this.$('.nav-user-status').on('click',_.bind(loginDialog.show, loginDialog));
        }
    });

    console.navbar = new core.console.NavBar();

})(core.console);

})(window.core);
