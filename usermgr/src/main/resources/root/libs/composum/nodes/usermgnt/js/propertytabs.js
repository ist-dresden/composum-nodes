(function () {
    'use strict';
    CPM.namespace('nodes.usermanagement');

    (function (usermanagement, browser, core) {

        usermanagement.PropertiesTab = browser.PropertiesTab.extend({

            initialize: function (options) {
                browser.PropertiesTab.prototype.initialize.call(this, options);
                this.$deleteButton = this.$('.table-toolbar .delete');
                this.$deleteButton.click(_.bind(this.deleteNode, this));
            },

            selectPath: function (path) {
                if (path) {
                    if (path.indexOf('/home/') === 0) {
                        usermanagement.setCurrentPath(path);
                    } else if (path.indexOf('/') === 0) {
                        window.open(core.getContextUrl('/bin/browser.html' + path));
                    }
                }
            },

            deleteNode: function () {
                var path = this.getPath();
                var dialog = usermanagement.getDeleteResourceDialog();
                dialog.show(function () {
                    dialog.setPath(path);
                }, _.bind(this.reload, this));
            }
        });

        usermanagement.ProfileTab = usermanagement.PropertiesTab.extend({

            getPath: function () {
                var path = usermanagement.getCurrentPath();
                return path ? (path + '/profile') : undefined;
            }
        });

        usermanagement.PreferencesTab = usermanagement.PropertiesTab.extend({

            getPath: function () {
                var path = usermanagement.getCurrentPath();
                return path ? (path + '/preferences') : undefined;
            }
        });

    })(CPM.nodes.usermanagement, CPM.namespace('nodes.browser'), CPM.core);

})();
