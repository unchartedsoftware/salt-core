/* */ 
define(['exports', 'aurelia-binding'], function (exports, _aureliaBinding) {
  'use strict';

  exports.__esModule = true;

  function _inherits(subClass, superClass) { if (typeof superClass !== 'function' && superClass !== null) { throw new TypeError('Super expression must either be null or a function, not ' + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var ModeBindingBehavior = (function () {
    function ModeBindingBehavior(mode) {
      _classCallCheck(this, ModeBindingBehavior);

      this.mode = mode;
    }

    ModeBindingBehavior.prototype.bind = function bind(binding, source, lookupFunctions) {
      binding.originalMode = binding.mode;
      binding.mode = this.mode;
    };

    ModeBindingBehavior.prototype.unbind = function unbind(binding, source) {
      binding.mode = binding.originalMode;
      binding.originalMode = null;
    };

    return ModeBindingBehavior;
  })();

  var OneTimeBindingBehavior = (function (_ModeBindingBehavior) {
    _inherits(OneTimeBindingBehavior, _ModeBindingBehavior);

    function OneTimeBindingBehavior() {
      _classCallCheck(this, OneTimeBindingBehavior);

      _ModeBindingBehavior.call(this, _aureliaBinding.bindingMode.oneTime);
    }

    return OneTimeBindingBehavior;
  })(ModeBindingBehavior);

  exports.OneTimeBindingBehavior = OneTimeBindingBehavior;

  var OneWayBindingBehavior = (function (_ModeBindingBehavior2) {
    _inherits(OneWayBindingBehavior, _ModeBindingBehavior2);

    function OneWayBindingBehavior() {
      _classCallCheck(this, OneWayBindingBehavior);

      _ModeBindingBehavior2.call(this, _aureliaBinding.bindingMode.oneWay);
    }

    return OneWayBindingBehavior;
  })(ModeBindingBehavior);

  exports.OneWayBindingBehavior = OneWayBindingBehavior;

  var TwoWayBindingBehavior = (function (_ModeBindingBehavior3) {
    _inherits(TwoWayBindingBehavior, _ModeBindingBehavior3);

    function TwoWayBindingBehavior() {
      _classCallCheck(this, TwoWayBindingBehavior);

      _ModeBindingBehavior3.call(this, _aureliaBinding.bindingMode.twoWay);
    }

    return TwoWayBindingBehavior;
  })(ModeBindingBehavior);

  exports.TwoWayBindingBehavior = TwoWayBindingBehavior;
});