/**
 *
 *
 */
(function(core) {
    'use strict';

    core.components = core.components || {};

(function(components) {

    components.SplitView = Backbone.View.extend({

        initialize: function(options) {
            this.id = this.$el.attr('id');
            this.profile = core.console.getProfile().get(this.id, 'split', {
                    horizontal: undefined,
                    vertical: undefined,
                    left: true,
                    top: true
                });
            this.horizontalSplit = core.getWidget(this.$el,
                '.split-pane.horizontal-split', core.components.HorizontalSplitPane);
            this.verticalSplit = core.getWidget(this.$el,
                '.split-pane.vertical-split', core.components.VerticalSplitPane);
            if (this.profile.left) {
                this.horizontalSplit.setPosition(this.horizontalSplit.checkPosition(this.profile.horizontal));
            } else {
                this.closeLeft(false);
            }
            if (this.profile.top) {
                this.verticalSplit.setPosition(this.verticalSplit.checkPosition(this.profile.vertical));
            } else {
                this.closeTop(false);
            }
            this.horizontalSplit.$el.on('resize', _.bind (this.onResize, this));
            this.verticalSplit.$el.on('resize', _.bind (this.onResize, this));
            this.$openLeft = this.$('.split-pane.horizontal-split > .open-left a');
            this.$closeLeft = this.$('.split-pane.horizontal-split > .split-pane-component.right-pane > .close-left a');
            this.$openTop = this.$('.split-pane.vertical-split  > .open-top a');
            this.$closeTop = this.$('.split-pane.vertical-split > .split-pane-component.bottom-pane > .close-top a');
            this.$openLeft.click(_.bind(this.openLeft, this));
            this.$closeLeft.click(_.bind(this.closeLeft, this));
            this.$openTop.click(_.bind(this.openTop, this));
            this.$closeTop.click(_.bind(this.closeTop, this));
        },

        openLeft: function(event) {
            this.profile.left = true;
            this.$el.addClass('left-open');
            this.horizontalSplit.setPosition(this.horizontalSplit.checkPosition(this.profile.horizontal));
            if (event) {
                event.preventDefault();
                this.stateChanged();
            }
            return false;
        },

        closeLeft: function(event) {
            this.profile.left = false;
            this.$el.removeClass('left-open');
            this.horizontalSplit.setPosition(0);
            if (event) {
                event.preventDefault();
                this.stateChanged();
            }
            return false;
        },

        openTop: function(event) {
            this.profile.top = true;
            this.$el.addClass('top-open');
            this.verticalSplit.setPosition(this.verticalSplit.checkPosition(this.profile.vertical));
            if (event) {
                event.preventDefault();
                this.stateChanged();
            }
            return false;
        },

        closeTop: function(event) {
            this.profile.top = false;
            this.$el.removeClass('top-open');
            this.verticalSplit.setPosition(0);
            if (event) {
                event.preventDefault();
                this.stateChanged();
            }
            return false;
        },

        onResize: function() {
            var last = _.clone(this.profile);
            if (this.profile.left) {
                this.profile.horizontal = this.horizontalSplit.getPosition();
            }
            if (this.profile.top) {
                this.profile.vertical = this.verticalSplit.getPosition();
            }
            if (!_.isEqual(last,this.profile)) {
                this.stateChanged();
            }
        },

        stateChanged: function() {
            core.console.getProfile().set(this.id, 'split', this.profile);
            this.horizontalSplit.stateChanged();
            this.verticalSplit.stateChanged();
        }
    });

    /**
     */
    components.SplitPane = Backbone.View.extend({

        initialize: function(options) {
            this.$components = this.$('> .split-pane-component');
            this.$first = $(this.$components[0]);
            this.$second = $(this.$components[1]);
            this.$divider = this.$('> .split-pane-divider');
            this.$el.splitPane();
        },

        checkPosition: function(position) {
            if (position !== undefined) {
                var minPosition = this.getMinPosition();
                var maxPosition = this.getMaxPosition();
                if (position < minPosition) {
                    position = minPosition;
                }
                if (position > maxPosition) {
                    position = maxPosition;
                }
            }
            return position;
        },

        stateChanged: function() {
            this.$first.children().each(function() {
                $(this).resize();
            });
            this.$second.children().each(function() {
                $(this).resize();
            });
        }
    });

    /**
     */
    components.HorizontalSplitPane = components.SplitPane.extend({

        getPosition: function() {
            var position = parseInt(this.$divider.css('left'));
            return position;
        },

        setPosition: function(position) {
            if (position !== undefined) {
                this.$first.css('width', position + 'px');
                this.$divider.css('left', position + 'px');
                this.$second.css('left', position + 'px');
            }
        },

        getMinPosition: function() {
            return parseInt(this.$first.css('min-width') || '0');
        },

        getMaxPosition: function() {
            var width = parseInt(this.$el.width());
            return width - parseInt(this.$second.css('min-width') || '0');
        }
    });

    /**
     */
    components.VerticalSplitPane = components.SplitPane.extend({

        getPosition: function() {
            var position = parseInt(this.$divider.css('top'));
            return position;
        },

        setPosition: function(position) {
            if (position !== undefined) {
                this.$first.css('height', position + 'px');
                this.$divider.css('top', position + 'px');
                this.$second.css('top', position + 'px');
            }
        },

        getMinPosition: function() {
            return parseInt(this.$first.css('min-height') || '0');
        },

        getMaxPosition: function() {
            var height = parseInt(this.$el.height());
            return height - parseInt(this.$second.css('min-height') || '0');
        }
    });

})(core.components);

})(window.core);
