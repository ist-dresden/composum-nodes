(function (user, core) {
    'use strict';

    user.ProfileDialog = core.components.FormDialog.extend({});

    user.openProfileDialog = function (userPath, config, initView, callback) {
        if (!/^.*\/profile$/.exec(userPath)) {
            userPath += '/profile';
        }
        core.openFormDialog('/libs/composum/nodes/commons/components/user/profile/dialog.html'
            + encodeURIComponent(userPath), user.ProfileDialog, config, initView, callback);
    };

})(CPM.namespace('nodes.commons.user'), CPM.core);