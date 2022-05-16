/**
 *
 *
 */
(function () {
    'use strict';
    CPM.namespace('nodes.pckgmgr');

    (function (pckgmgr, console, core) {

        pckgmgr.const = {
            mode: {
                jcrpckg: 'jcrpckg',
                regpckg: 'regpckg'
            },
            css: {
                tree: {
                    tabs: {
                        base: 'nodes-pckgmgr-tree-tabs',
                        _head: '_head',
                        _body: '_body',
                        _tab: '_head-tab'
                    }
                }
            },
            uri: {
                base: '/libs/composum/nodes/pckgmgr',
                exec: function (operation, ext) {
                    return '/bin/cpm/package.' + operation + '.' + (ext ? ext : 'json');
                }
            }
        }

        pckgmgr.mode = {
            current: undefined,
            profile: function () {
                return pckgmgr.mode.current === 'pckgmgr' ? 'pckgmgr' : pckgmgr.regpckg.mode.profile();
            },
            exec: {
                uri: function (operation) {
                    return pckgmgr.mode.exec[pckgmgr.mode.current](operation);
                },
                jcrpckg: function (operation) {
                    return '/bin/cpm/package.' + operation + '.json';
                },
                regpckg: function (operation) {
                    pckgmgr.mode.exec.jcrpckg(operation);
                }
            },
            tree: {
                uri: function () {
                    return pckgmgr.mode.tree[pckgmgr.mode.current]();
                },
                jcrpckg: function () {
                    return '/bin/cpm/package.tree.json';
                },
                regpckg: function () {
                    return pckgmgr.regpckg.mode.tree.uri();
                }
            }
        };

        pckgmgr.current = {};

        pckgmgr.getCurrentPath = function () {
            return pckgmgr.current ? pckgmgr.current.path : undefined;
        };

        pckgmgr.setCurrentPath = function (path) {
            if (!pckgmgr.current || pckgmgr.current.path !== path) {
                if (path) {
                    var regpckg = pckgmgr.util.namespace(path);
                    core.getJson(pckgmgr.mode.tree.uri() + core.encodePath(path),
                        undefined, undefined, _.bind(function (result) {
                            var pathMatch = pckgmgr.pattern.file.exec(path);
                            pckgmgr.current = {
                                registry: (regpckg ? regpckg.namespace : undefined),
                                path: path,
                                group: result.responseJSON.packageid ? result.responseJSON.packageid.group : undefined,
                                // group: pathMatch ? pathMatch[2] : undefined,
                                name: result.responseJSON.packageid ? result.responseJSON.packageid.name : undefined,
                                // name: pathMatch ? pathMatch[3] : undefined,
                                version: result.responseJSON.packageid ? result.responseJSON.packageid.version : undefined,
                                // version: pathMatch ? pathMatch[4] : undefined,
                                extension: pathMatch ? pathMatch[5] : undefined,
                                includeVersions: result.responseJSON.definition ? result.responseJSON.definition.includeVersions : undefined,
                                node: result.responseJSON,
                                viewUrl: core.getContextUrl('/bin/packages.view.html'
                                    + core.encodePath(path)),
                                nodeUrl: core.getContextUrl('/bin/packages.html'
                                    + core.encodePath(path)),
                                downloadUrl: pathMatch || result.responseJSON.packageid
                                    ? core.getContextUrl('/bin/cpm/package.download.zip' + core.encodePath(path))
                                    : ''
                            };
                            core.console.getProfile().set(pckgmgr.mode.profile(), 'current', path);
                            if (history.replaceState) {
                                history.replaceState(pckgmgr.current.path, name, pckgmgr.current.nodeUrl);
                            }
                            $(document).trigger("path:selected", [path]);
                        }, this));
                } else {
                    pckgmgr.current = undefined;
                    $(document).trigger("path:selected", [path]);
                }
            }
        };

        pckgmgr.Pckgmgr = console.components.SplitView.extend({

            initialize: function (options) {
                console.components.SplitView.prototype.initialize.apply(this, [options]);
                var t = pckgmgr.const.css.tree.tabs;
                var m = pckgmgr.const.mode;
                this.$treeTabs = this.$('.' + t.base + t._head);
                this.$treeBody = this.$('.' + t.base + t._body);
                this.$treeTabs.find('.' + m.jcrpckg).click(_.bind(this.selectJcrpckgTab, this));
                this.$treeTabs.find('.' + m.regpckg).click(_.bind(this.selectRegpckgTab, this));
                $(document).on('path:select', _.bind(this.onPathSelect, this));
                $(document).on('path:selected', _.bind(this.onPathSelected, this));
                core.unauthorizedDelegate = core.console.authorize;
                this.selectMode(core.console.getProfile().get('pckgmgr', 'mode', 'jcrpckg'));
            },

            selectJcrpckgTab: function () {
                this.selectMode('jcrpckg');
            },

            selectRegpckgTab: function () {
                this.selectMode('regpckg');
            },

            selectMode: function (mode) {
                if (pckgmgr.mode.current !== mode) {
                    pckgmgr.mode.current = mode;
                    core.ajaxGet(pckgmgr.const.uri.exec('mode.' + mode), {}, undefined, undefined,
                        _.bind(function () {
                            core.getHtml(pckgmgr.const.uri.base + '/' + mode + '/tree.html',
                                _.bind(function (html) {
                                    var t = pckgmgr.const.css.tree.tabs;
                                    this.$treeTabs.find('.' + t.base + t._tab).removeClass('active');
                                    this.$treeTabs.find('.' + t.base + t._tab + '.' + mode + '').addClass('active');
                                    this.$treeBody.html(html);
                                    pckgmgr[mode].mode.tree.setup();
                                    core.console.getProfile().set('pckgmgr', 'mode', mode);
                                    this.switchUpload(mode)
                                }, this));
                        }, this));
                }
            },

            /** Switches the registry input in the package upload dialog on or off, according to the mode. */
            switchUpload: function(mode) {
                let registrySelector = this.$('.pckg-regpckg-mode-mandatory');
                if (mode != 'regpckg') {
                    this.$('.pckg-regpckg-mode-only').addClass('hidden');
                    registrySelector.attr('disabled', 'disabled');
                    registrySelector.removeAttr('data-rules');
                    registrySelector.removeClass('widget');
                } else {
                    this.$('.pckg-regpckg-mode-only').removeClass('hidden');
                    registrySelector.removeAttr('disabled');
                    registrySelector.attr('data-rules', 'mandatory');
                    registrySelector.addClass('widget');
                }
                registrySelector.val('');
            },

            onPathSelect: function (event, path) {
                if (!path) {
                    path = event.data.path;
                }
                pckgmgr.setCurrentPath(path);
            },

            onPathSelected: function (event, path) {
                pckgmgr[pckgmgr.mode.current].tree.selectNode(path, _.bind(function (path) {
                    pckgmgr[pckgmgr.mode.current].tree.actions.refreshNodeState();
                }, this));
            }
        });

        pckgmgr.pckgmgr = core.getView('#pckgmgr', pckgmgr.Pckgmgr);

        //
        // detail view (console)
        //

        pckgmgr.detailViewTabTypes = [{
            selector: '> .package',
            tabType: pckgmgr.JcrPackageTab
        }, {
            selector: '> .filters',
            tabType: pckgmgr.FiltersTab
        }, {
            selector: '> .coverage',
            tabType: pckgmgr.CoverageTab
        }, {
            selector: '> .options',
            tabType: pckgmgr.OptionsTab
        }, {
            selector: '> .group',
            tabType: pckgmgr.GroupTab
        }, {
            // the fallback to the basic implementation as a default rule
            selector: '> div',
            tabType: core.console.DetailTab
        }];

        /**
         * the node view (node detail) which controls the node view tabs
         */
        pckgmgr.DetailView = core.console.DetailView.extend({

            getProfileId: function () {
                return 'pckgmgr';
            },

            getCurrentPath: function () {
                return pckgmgr.current ? pckgmgr.current.path : undefined;
            },

            getViewUri: function () {
                return pckgmgr.current.viewUrl;
            },

            getTabUri: function (name) {
                return '/bin/packages.tab.' + name + '.html';
            },

            getTabTypes: function () {
                return pckgmgr.detailViewTabTypes;
            },

            initialize: function (options) {
                core.console.DetailView.prototype.initialize.apply(this, [options]);
            }
        });

        pckgmgr.detailView = core.getView('#pckgmgr-view', pckgmgr.DetailView);

    })(CPM.nodes.pckgmgr, CPM.console, CPM.core);

})();
