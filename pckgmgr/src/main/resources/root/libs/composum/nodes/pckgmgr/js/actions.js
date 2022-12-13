/**
 * pathRef{path, name, type}
 * pckgRef{[namespace,] group, name[, version]}
 */
(function (pckgmgr, console, core) {
    'use strict';

    pckgmgr.pattern = {
        path: new RegExp('^(/@([^/]*))?(/.*)$'),
        file: new RegExp('^/((.+)/)?([^/]+)-([^/]+)\.(zip|jar)$')
    };

    pckgmgr.util = {

        namespace: function (path) {
            var matcher = pckgmgr.pattern.path.exec(path);
            return matcher ? {
                namespace: matcher[2], path: matcher[3]
            } : undefined;
        }
    }

    pckgmgr.actions = {

        createPackage: function (parentPathRef) {
            var dialog = pckgmgr.getCreatePackageDialog();
            dialog.show(_.bind(function () {
                var parentPath = parentPathRef.path;
                if (parentPathRef.type === 'package') {
                    parentPath = core.getParentPath(parentPath);
                }
                var regpckg = pckgmgr.util.namespace(parentPath);
                if (regpckg) {
                    // FIXME dialog.initRegistry(regpckg.namespace);
                    parentPath = regpckg.path;
                }
                if (parentPath) {
                    dialog.initGroup(parentPath.substring(1));
                }
            }, this));
        },

        deletePackage: function (pckgRef) {
            if (pckgRef.name) {
                var dialog = pckgmgr.getDeletePackageDialog();
                dialog.show(_.bind(function () {
                    dialog.setPackage(pckgRef);
                }, this));
            }
        },

        uploadPackage: function () {
            var dialog = pckgmgr.getUploadPackageDialog();
            dialog.show(_.bind(function () {
            }, this));
        }
    };

})(CPM.namespace('nodes.pckgmgr'), CPM.console, CPM.core);
