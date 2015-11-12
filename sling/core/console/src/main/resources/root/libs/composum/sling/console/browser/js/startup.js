/**
 * final startup code which needs all components present
 *
 */
'use strict';

(function(core) {

(function(browser) {

    var navMode = core.console.getProfile().get('browser', 'navigation');
    if (navMode == 'favorites') {
        browser.treeActions.toggleFavorites();
    }

})(core.browser);

})(window.core);
