/* */ 
define(['exports', './binding-signaler'], function (exports, _bindingSignaler) {
  'use strict';

  exports.__esModule = true;

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var SignalBindingBehavior = (function () {
    SignalBindingBehavior.inject = function inject() {
      return [_bindingSignaler.BindingSignaler];
    };

    function SignalBindingBehavior(bindingSignaler) {
      _classCallCheck(this, SignalBindingBehavior);

      this.signals = bindingSignaler.signals;
    }

    SignalBindingBehavior.prototype.bind = function bind(binding, source, name) {
      if (!binding.updateTarget) {
        throw new Error('Only property bindings and string interpolation bindings can be signaled.  Trigger, delegate and call bindings cannot be signaled.');
      }
      var bindings = this.signals[name] || (this.signals[name] = []);
      bindings.push(binding);
      binding.signalName = name;
    };

    SignalBindingBehavior.prototype.unbind = function unbind(binding, source) {
      var name = binding.signalName;
      binding.signalName = null;
      var bindings = this.signals[name];
      bindings.splice(bindings.indexOf(binding), 1);
    };

    return SignalBindingBehavior;
  })();

  exports.SignalBindingBehavior = SignalBindingBehavior;
});