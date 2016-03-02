/* */ 
define(['exports', 'core-js', 'aurelia-pal', 'aurelia-history'], function (exports, _coreJs, _aureliaPal, _aureliaHistory) {
  'use strict';

  exports.__esModule = true;

  var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

  exports.configure = configure;

  function _inherits(subClass, superClass) { if (typeof superClass !== 'function' && superClass !== null) { throw new TypeError('Super expression must either be null or a function, not ' + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var LinkHandler = (function () {
    function LinkHandler() {
      _classCallCheck(this, LinkHandler);
    }

    LinkHandler.prototype.activate = function activate(history) {};

    LinkHandler.prototype.deactivate = function deactivate() {};

    return LinkHandler;
  })();

  exports.LinkHandler = LinkHandler;

  var DefaultLinkHandler = (function (_LinkHandler) {
    _inherits(DefaultLinkHandler, _LinkHandler);

    function DefaultLinkHandler() {
      var _this = this;

      _classCallCheck(this, DefaultLinkHandler);

      _LinkHandler.call(this);

      this.handler = function (e) {
        var _DefaultLinkHandler$getEventInfo = DefaultLinkHandler.getEventInfo(e);

        var shouldHandleEvent = _DefaultLinkHandler$getEventInfo.shouldHandleEvent;
        var href = _DefaultLinkHandler$getEventInfo.href;

        if (shouldHandleEvent) {
          e.preventDefault();
          _this.history.navigate(href);
        }
      };
    }

    DefaultLinkHandler.prototype.activate = function activate(history) {
      if (history._hasPushState) {
        this.history = history;
        _aureliaPal.DOM.addEventListener('click', this.handler, true);
      }
    };

    DefaultLinkHandler.prototype.deactivate = function deactivate() {
      _aureliaPal.DOM.removeEventListener('click', this.handler);
    };

    DefaultLinkHandler.getEventInfo = function getEventInfo(event) {
      var info = {
        shouldHandleEvent: false,
        href: null,
        anchor: null
      };

      var target = DefaultLinkHandler.findClosestAnchor(event.target);
      if (!target || !DefaultLinkHandler.targetIsThisWindow(target)) {
        return info;
      }

      if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) {
        return info;
      }

      var href = target.getAttribute('href');
      info.anchor = target;
      info.href = href;

      var leftButtonClicked = event.which === 1;
      var isRelative = href && !(href.charAt(0) === '#' || /^[a-z]+:/i.test(href));

      info.shouldHandleEvent = leftButtonClicked && isRelative;
      return info;
    };

    DefaultLinkHandler.findClosestAnchor = function findClosestAnchor(el) {
      while (el) {
        if (el.tagName === 'A') {
          return el;
        }

        el = el.parentNode;
      }
    };

    DefaultLinkHandler.targetIsThisWindow = function targetIsThisWindow(target) {
      var targetWindow = target.getAttribute('target');
      var win = _aureliaPal.PLATFORM.global;

      return !targetWindow || targetWindow === win.name || targetWindow === '_self' || targetWindow === 'top' && win === win.top;
    };

    return DefaultLinkHandler;
  })(LinkHandler);

  exports.DefaultLinkHandler = DefaultLinkHandler;

  function configure(config) {
    config.singleton(_aureliaHistory.History, BrowserHistory);
    config.transient(LinkHandler, DefaultLinkHandler);
  }

  var BrowserHistory = (function (_History) {
    _inherits(BrowserHistory, _History);

    _createClass(BrowserHistory, null, [{
      key: 'inject',
      value: [LinkHandler],
      enumerable: true
    }]);

    function BrowserHistory(linkHandler) {
      _classCallCheck(this, BrowserHistory);

      _History.call(this);

      this._isActive = false;
      this._checkUrlCallback = this._checkUrl.bind(this);

      this.location = _aureliaPal.PLATFORM.location;
      this.history = _aureliaPal.PLATFORM.history;
      this.linkHandler = linkHandler;
    }

    BrowserHistory.prototype.activate = function activate(options) {
      if (this._isActive) {
        throw new Error('History has already been activated.');
      }

      var wantsPushState = !!options.pushState;

      this._isActive = true;
      this.options = Object.assign({}, { root: '/' }, this.options, options);

      this.root = ('/' + this.options.root + '/').replace(rootStripper, '/');

      this._wantsHashChange = this.options.hashChange !== false;
      this._hasPushState = !!(this.options.pushState && this.history && this.history.pushState);

      var eventName = undefined;
      if (this._hasPushState) {
        eventName = 'popstate';
      } else if (this._wantsHashChange) {
        eventName = 'hashchange';
      }

      _aureliaPal.PLATFORM.addEventListener(eventName, this._checkUrlCallback);

      if (this._wantsHashChange && wantsPushState) {
        var loc = this.location;
        var atRoot = loc.pathname.replace(/[^\/]$/, '$&/') === this.root;

        if (!this._hasPushState && !atRoot) {
          this.fragment = this._getFragment(null, true);
          this.location.replace(this.root + this.location.search + '#' + this.fragment);

          return true;
        } else if (this._hasPushState && atRoot && loc.hash) {
            this.fragment = this._getHash().replace(routeStripper, '');
            this.history.replaceState({}, _aureliaPal.DOM.title, this.root + this.fragment + loc.search);
          }
      }

      if (!this.fragment) {
        this.fragment = this._getFragment();
      }

      this.linkHandler.activate(this);

      if (!this.options.silent) {
        return this._loadUrl();
      }
    };

    BrowserHistory.prototype.deactivate = function deactivate() {
      _aureliaPal.PLATFORM.removeEventListener('popstate', this._checkUrlCallback);
      _aureliaPal.PLATFORM.removeEventListener('hashchange', this._checkUrlCallback);
      this._isActive = false;
      this.linkHandler.deactivate();
    };

    BrowserHistory.prototype.navigate = function navigate(fragment) {
      var _ref = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1];

      var _ref$trigger = _ref.trigger;
      var trigger = _ref$trigger === undefined ? true : _ref$trigger;
      var _ref$replace = _ref.replace;
      var replace = _ref$replace === undefined ? false : _ref$replace;

      if (fragment && absoluteUrl.test(fragment)) {
        this.location.href = fragment;
        return true;
      }

      if (!this._isActive) {
        return false;
      }

      fragment = this._getFragment(fragment || '');

      if (this.fragment === fragment && !replace) {
        return false;
      }

      this.fragment = fragment;

      var url = this.root + fragment;

      if (fragment === '' && url !== '/') {
        url = url.slice(0, -1);
      }

      if (this._hasPushState) {
        url = url.replace('//', '/');
        this.history[replace ? 'replaceState' : 'pushState']({}, _aureliaPal.DOM.title, url);
      } else if (this._wantsHashChange) {
        updateHash(this.location, fragment, replace);
      } else {
        return this.location.assign(url);
      }

      if (trigger) {
        return this._loadUrl(fragment);
      }
    };

    BrowserHistory.prototype.navigateBack = function navigateBack() {
      this.history.back();
    };

    BrowserHistory.prototype.setTitle = function setTitle(title) {
      _aureliaPal.DOM.title = title;
    };

    BrowserHistory.prototype._getHash = function _getHash() {
      return this.location.hash.substr(1);
    };

    BrowserHistory.prototype._getFragment = function _getFragment(fragment, forcePushState) {
      var root = undefined;

      if (!fragment) {
        if (this._hasPushState || !this._wantsHashChange || forcePushState) {
          fragment = this.location.pathname + this.location.search;
          root = this.root.replace(trailingSlash, '');
          if (!fragment.indexOf(root)) {
            fragment = fragment.substr(root.length);
          }
        } else {
          fragment = this._getHash();
        }
      }

      return '/' + fragment.replace(routeStripper, '');
    };

    BrowserHistory.prototype._checkUrl = function _checkUrl() {
      var current = this._getFragment();
      if (current !== this.fragment) {
        this._loadUrl();
      }
    };

    BrowserHistory.prototype._loadUrl = function _loadUrl(fragmentOverride) {
      var fragment = this.fragment = this._getFragment(fragmentOverride);

      return this.options.routeHandler ? this.options.routeHandler(fragment) : false;
    };

    return BrowserHistory;
  })(_aureliaHistory.History);

  exports.BrowserHistory = BrowserHistory;

  var routeStripper = /^#?\/*|\s+$/g;

  var rootStripper = /^\/+|\/+$/g;

  var trailingSlash = /\/$/;

  var absoluteUrl = /^([a-z][a-z0-9+\-.]*:)?\/\//i;

  function updateHash(location, fragment, replace) {
    if (replace) {
      var href = location.href.replace(/(javascript:|#).*$/, '');
      location.replace(href + '#' + fragment);
    } else {
      location.hash = '#' + fragment;
    }
  }
});