/* */ 
define(["exports"], function (exports) {
  "use strict";

  exports.__esModule = true;

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

  var NullRepeatStrategy = (function () {
    function NullRepeatStrategy() {
      _classCallCheck(this, NullRepeatStrategy);
    }

    NullRepeatStrategy.prototype.instanceChanged = function instanceChanged(repeat, items) {
      repeat.viewSlot.removeAll(true);
    };

    NullRepeatStrategy.prototype.getCollectionObserver = function getCollectionObserver(observerLocator, items) {};

    return NullRepeatStrategy;
  })();

  exports.NullRepeatStrategy = NullRepeatStrategy;
});