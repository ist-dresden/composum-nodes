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

    //
    // detail view (generalized base class)
    //

    /**
     * the base 'class' for all detail tabs
     */
    console.DetailTab = Backbone.View.extend({
    });

    /**
     * the node view (node detail) which controls the node view tabs
     */
    console.DetailView = Backbone.View.extend({

        // 'abstract' members...

        getProfileId: function() {
            return undefined;
        },

        getCurrentPath: function() {
            return undefined;
        },

        getViewUri: function() {
            return undefined;
        },

        getTabUri: function(name) {
            return undefined;
        },

        getTabTypes: function() {
            return [{
                // the fallback to the basic implementation as a default rule
                selector: '> div',
                tabType: console.DetailTab
            }];
        },

        // detail view implementation

        initialize: function(options) {
            $(document).on('path:selected', _.bind(this.reload, this));
        },
        
        /**
         * (re)load the content with the view for the current node ('console.getCurrentPath()')
         */
        reload: function() {
            this.path = this.getCurrentPath();
            if (this.path) {
                // AJAX load for the current path with the 'viewUrl' from 'console.current'
                this.$el.load(this.getViewUri(), _.bind (function() {
                    // initialize detail view with the loaded content
                    this.$detailView = this.$('.detail-view');
                    this.$detailTabs = this.$detailView.find('.detail-tabs');
                    this.$detailContent = this.$detailView.find('.detail-content');
                    // add the click handler to the tab toolbar links
                    this.$detailTabs.find('a').click(_.bind (this.tabSelected, this));
                    // get the group key of the last view from profile and restore this tab state if possible
                    var group = core.console.getProfile().get(this.getProfileId(), 'detailTab');
                    var $tab;
                    if (group) {
                        // determinte the last view by the group id if such a view is available
                        $tab = this.$detailView.find('a[data-group="' + group + '"]');
                    }
                    if (! $tab || $tab.length < 1) {
                        // if the group of the last view is not available use the view of the first tab
                        $tab = this.$detailTabs.find('a');
                    }
                    // get the tab key from the links anchor and select the tab
                    var tab = $tab.attr('href').substring(1);
                    this.selectTab(tab, group); // remember the group key(!)
                    if (_.isFunction(this.onReload)) {
                        this.onReload();
                    }
                }, this));
            } else {
                this.$el.html(''); // clear the view if nothing selected
            }
        },

        /**
         * select a tab by the tabs anchor name; remember the group of the tab in the users profile,
         * remember the group given here as parameter if this parameter is not undefined
         */
        selectTab: function(name, group) {
            if (!name) {
                name = 'properties';
            }
            var path = this.getCurrentPath();
            if (path) {
                var href=this.getTabUri(name) + window.core.encodePath(path)
                this.$detailContent.load(core.getContextUrl(href), _.bind (function() {
                    this.$detailTabs.find('a.active').removeClass('active');
                    var $item = this.$detailTabs.find('a[href="#' + name + '"]');
                    $item.addClass('active');
                    if (!group) {
                        group = $item.attr('data-group');
                    }
                    core.console.getProfile().set(this.getProfileId(), 'detailTab', group);
                    // initialize the new view
                    this.viewWidget = undefined;
                    var tabTypes = this.getTabTypes();
                    for (var i=0; !this.viewWidget && i < tabTypes.length; i++) {
                        var type = tabTypes[i];
                        this.viewWidget = core.getWidget (this.$detailContent, type.selector, type.tabType);
                    }
                    if (this.viewWidget) {
                        if (_.isFunction(this.viewWidget.reload)) {
                            this.viewWidget.reload();
                        }
                    }
                }, this));
            }
        },

        /**
         * the event handler for the tab actions (button links) calls 'selectTab' with the links anchor
         */
        tabSelected: function(event) {
            event.preventDefault();
            var $action = $(event.currentTarget).closest('a');
            var tab = $action.attr('href').substring(1);
            this.selectTab(tab);
            return false;
        }
    });

})(core.console);

})(window.core);
