/* */ 
define(['exports', 'aurelia-binding'], function (exports, _aureliaBinding) {
  'use strict';

  exports.__esModule = true;

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var BindingSignaler = (function () {
    function BindingSignaler() {
      _classCallCheck(this, BindingSignaler);

      this.signals = {};
    }

    BindingSignaler.prototype.signal = function signal(name) {
      var bindings = this.signals[name];
      if (!bindings) {
        return;
      }
      var i = bindings.length;
      while (i--) {
        bindings[i].call(_aureliaBinding.sourceContext);
      }
    };

    return BindingSignaler;
  })();

  exports.BindingSignaler = BindingSignaler;
});