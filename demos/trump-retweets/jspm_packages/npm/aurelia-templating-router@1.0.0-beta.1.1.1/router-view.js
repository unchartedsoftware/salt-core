/* */ 
define(['exports', 'aurelia-dependency-injection', 'aurelia-templating', 'aurelia-router', 'aurelia-metadata', 'aurelia-pal'], function (exports, _aureliaDependencyInjection, _aureliaTemplating, _aureliaRouter, _aureliaMetadata, _aureliaPal) {
  'use strict';

  exports.__esModule = true;

  var _createDecoratedClass = (function () { function defineProperties(target, descriptors, initializers) { for (var i = 0; i < descriptors.length; i++) { var descriptor = descriptors[i]; var decorators = descriptor.decorators; var key = descriptor.key; delete descriptor.key; delete descriptor.decorators; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor || descriptor.initializer) descriptor.writable = true; if (decorators) { for (var f = 0; f < decorators.length; f++) { var decorator = decorators[f]; if (typeof decorator === 'function') { descriptor = decorator(target, key, descriptor) || descriptor; } else { throw new TypeError('The decorator for method ' + descriptor.key + ' is of the invalid type ' + typeof decorator); } } if (descriptor.initializer !== undefined) { initializers[key] = descriptor; continue; } } Object.defineProperty(target, key, descriptor); } } return function (Constructor, protoProps, staticProps, protoInitializers, staticInitializers) { if (protoProps) defineProperties(Constructor.prototype, protoProps, protoInitializers); if (staticProps) defineProperties(Constructor, staticProps, staticInitializers); return Constructor; }; })();

  function _defineDecoratedPropertyDescriptor(target, key, descriptors) { var _descriptor = descriptors[key]; if (!_descriptor) return; var descriptor = {}; for (var _key in _descriptor) descriptor[_key] = _descriptor[_key]; descriptor.value = descriptor.initializer ? descriptor.initializer.call(target) : undefined; Object.defineProperty(target, key, descriptor); }

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var SwapStrategies = (function () {
    function SwapStrategies() {
      _classCallCheck(this, SwapStrategies);
    }

    SwapStrategies.prototype.before = function before(viewSlot, previousView, callback) {
      var promise = Promise.resolve(callback());

      if (previousView !== undefined) {
        return promise.then(function () {
          return viewSlot.remove(previousView, true);
        });
      }

      return promise;
    };

    SwapStrategies.prototype['with'] = function _with(viewSlot, previousView, callback) {
      var promise = Promise.resolve(callback());

      if (previousView !== undefined) {
        return Promise.all([viewSlot.remove(previousView, true), promise]);
      }

      return promise;
    };

    SwapStrategies.prototype.after = function after(viewSlot, previousView, callback) {
      return Promise.resolve(viewSlot.removeAll(true)).then(callback);
    };

    return SwapStrategies;
  })();

  var swapStrategies = new SwapStrategies();

  var RouterView = (function () {
    var _instanceInitializers = {};

    _createDecoratedClass(RouterView, [{
      key: 'swapOrder',
      decorators: [_aureliaTemplating.bindable],
      initializer: null,
      enumerable: true
    }], null, _instanceInitializers);

    function RouterView(element, container, viewSlot, router, viewLocator) {
      _classCallCheck(this, _RouterView);

      _defineDecoratedPropertyDescriptor(this, 'swapOrder', _instanceInitializers);

      this.element = element;
      this.container = container;
      this.viewSlot = viewSlot;
      this.router = router;
      this.viewLocator = viewLocator;
      this.router.registerViewPort(this, this.element.getAttribute('name'));
    }

    RouterView.prototype.created = function created(owningView) {
      this.owningView = owningView;
    };

    RouterView.prototype.bind = function bind(bindingContext, overrideContext) {
      this.container.viewModel = bindingContext;
      this.overrideContext = overrideContext;
    };

    RouterView.prototype.process = function process(viewPortInstruction, waitToSwap) {
      var _this = this;

      var component = viewPortInstruction.component;
      var childContainer = component.childContainer;
      var viewModel = component.viewModel;
      var viewModelResource = component.viewModelResource;
      var metadata = viewModelResource.metadata;

      var viewStrategy = this.viewLocator.getViewStrategy(component.view || viewModel);
      if (viewStrategy) {
        viewStrategy.makeRelativeTo(_aureliaMetadata.Origin.get(component.router.container.viewModel.constructor).moduleId);
      }

      return metadata.load(childContainer, viewModelResource.value, null, viewStrategy, true).then(function (viewFactory) {
        viewPortInstruction.controller = metadata.create(childContainer, _aureliaTemplating.BehaviorInstruction.dynamic(_this.element, viewModel, viewFactory));

        if (waitToSwap) {
          return;
        }

        _this.swap(viewPortInstruction);
      });
    };

    RouterView.prototype.swap = function swap(viewPortInstruction) {
      var _this2 = this;

      var previousView = this.view;
      var viewSlot = this.viewSlot;
      var swapStrategy = undefined;

      swapStrategy = this.swapOrder in swapStrategies ? swapStrategies[this.swapOrder] : swapStrategies.after;

      swapStrategy(viewSlot, previousView, function () {
        viewPortInstruction.controller.automate(_this2.overrideContext, _this2.owningView);
        return viewSlot.add(viewPortInstruction.controller.view);
      });

      this.view = viewPortInstruction.controller.view;
    };

    var _RouterView = RouterView;
    RouterView = _aureliaDependencyInjection.inject(_aureliaPal.DOM.Element, _aureliaDependencyInjection.Container, _aureliaTemplating.ViewSlot, _aureliaRouter.Router, _aureliaTemplating.ViewLocator)(RouterView) || RouterView;
    RouterView = _aureliaTemplating.noView(RouterView) || RouterView;
    RouterView = _aureliaTemplating.customElement('router-view')(RouterView) || RouterView;
    return RouterView;
  })();

  exports.RouterView = RouterView;
});