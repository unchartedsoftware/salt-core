/* */ 
define(['exports', './compose', './if', './with', './repeat', './show', './sanitize-html', './replaceable', './focus', './compile-spy', './view-spy', 'aurelia-templating', './dynamic-element', './css-resource', 'aurelia-pal', './html-sanitizer', './binding-mode-behaviors', './throttle-binding-behavior', './debounce-binding-behavior', './signal-binding-behavior', './binding-signaler', './update-trigger-binding-behavior'], function (exports, _compose, _if, _with, _repeat, _show, _sanitizeHtml, _replaceable, _focus, _compileSpy, _viewSpy, _aureliaTemplating, _dynamicElement, _cssResource, _aureliaPal, _htmlSanitizer, _bindingModeBehaviors, _throttleBindingBehavior, _debounceBindingBehavior, _signalBindingBehavior, _bindingSignaler, _updateTriggerBindingBehavior) {
  'use strict';

  exports.__esModule = true;

  function configure(config) {
    if (_aureliaPal.FEATURE.shadowDOM) {
      _aureliaPal.DOM.injectStyles('body /deep/ .aurelia-hide { display:none !important; }');
    } else {
      _aureliaPal.DOM.injectStyles('.aurelia-hide { display:none !important; }');
    }

    config.globalResources('./compose', './if', './with', './repeat', './show', './replaceable', './sanitize-html', './focus', './compile-spy', './view-spy', './binding-mode-behaviors', './throttle-binding-behavior', './debounce-binding-behavior', './signal-binding-behavior', './update-trigger-binding-behavior');

    var viewEngine = config.container.get(_aureliaTemplating.ViewEngine);
    var loader = config.aurelia.loader;

    viewEngine.addResourcePlugin('.html', {
      'fetch': function fetch(address) {
        return loader.loadTemplate(address).then(function (registryEntry) {
          var _ref;

          var bindable = registryEntry.template.getAttribute('bindable');
          var elementName = address.replace('.html', '');
          var index = elementName.lastIndexOf('/');

          if (index !== 0) {
            elementName = elementName.substring(index + 1);
          }

          if (bindable) {
            bindable = bindable.split(',').map(function (x) {
              return x.trim();
            });
            registryEntry.template.removeAttribute('bindable');
          } else {
            bindable = [];
          }

          return _ref = {}, _ref[elementName] = _dynamicElement._createDynamicElement(elementName, address, bindable), _ref;
        });
      }
    });

    viewEngine.addResourcePlugin('.css', {
      'fetch': function fetch(address) {
        var _ref2;

        return _ref2 = {}, _ref2[address] = _cssResource._createCSSResource(address), _ref2;
      }
    });
  }

  exports.Compose = _compose.Compose;
  exports.If = _if.If;
  exports.With = _with.With;
  exports.Repeat = _repeat.Repeat;
  exports.Show = _show.Show;
  exports.HTMLSanitizer = _htmlSanitizer.HTMLSanitizer;
  exports.SanitizeHTMLValueConverter = _sanitizeHtml.SanitizeHTMLValueConverter;
  exports.Replaceable = _replaceable.Replaceable;
  exports.Focus = _focus.Focus;
  exports.CompileSpy = _compileSpy.CompileSpy;
  exports.ViewSpy = _viewSpy.ViewSpy;
  exports.configure = configure;
  exports.OneTimeBindingBehavior = _bindingModeBehaviors.OneTimeBindingBehavior;
  exports.OneWayBindingBehavior = _bindingModeBehaviors.OneWayBindingBehavior;
  exports.TwoWayBindingBehavior = _bindingModeBehaviors.TwoWayBindingBehavior;
  exports.ThrottleBindingBehavior = _throttleBindingBehavior.ThrottleBindingBehavior;
  exports.DebounceBindingBehavior = _debounceBindingBehavior.DebounceBindingBehavior;
  exports.SignalBindingBehavior = _signalBindingBehavior.SignalBindingBehavior;
  exports.BindingSignaler = _bindingSignaler.BindingSignaler;
  exports.UpdateTriggerBindingBehavior = _updateTriggerBindingBehavior.UpdateTriggerBindingBehavior;
});