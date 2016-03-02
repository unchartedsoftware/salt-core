/* */ 
define(['exports', 'aurelia-dependency-injection', 'aurelia-templating', 'aurelia-pal'], function (exports, _aureliaDependencyInjection, _aureliaTemplating, _aureliaPal) {
  'use strict';

  exports.__esModule = true;

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var Show = (function () {
    function Show(element, animator) {
      _classCallCheck(this, _Show);

      this.element = element;
      this.animator = animator;
    }

    Show.prototype.valueChanged = function valueChanged(newValue) {
      if (newValue) {
        this.animator.removeClass(this.element, 'aurelia-hide');
      } else {
        this.animator.addClass(this.element, 'aurelia-hide');
      }
    };

    Show.prototype.bind = function bind(bindingContext) {
      this.valueChanged(this.value);
    };

    var _Show = Show;
    Show = _aureliaDependencyInjection.inject(_aureliaPal.DOM.Element, _aureliaTemplating.Animator)(Show) || Show;
    Show = _aureliaTemplating.customAttribute('show')(Show) || Show;
    return Show;
  })();

  exports.Show = Show;
});