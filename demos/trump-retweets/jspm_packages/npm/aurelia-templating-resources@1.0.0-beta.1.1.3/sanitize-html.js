/* */ 
define(['exports', 'aurelia-binding', 'aurelia-dependency-injection', './html-sanitizer'], function (exports, _aureliaBinding, _aureliaDependencyInjection, _htmlSanitizer) {
  'use strict';

  exports.__esModule = true;

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var SanitizeHTMLValueConverter = (function () {
    function SanitizeHTMLValueConverter(sanitizer) {
      _classCallCheck(this, _SanitizeHTMLValueConverter);

      this.sanitizer = sanitizer;
    }

    SanitizeHTMLValueConverter.prototype.toView = function toView(untrustedMarkup) {
      if (untrustedMarkup === null || untrustedMarkup === undefined) {
        return null;
      }

      return this.sanitizer.sanitize(untrustedMarkup);
    };

    var _SanitizeHTMLValueConverter = SanitizeHTMLValueConverter;
    SanitizeHTMLValueConverter = _aureliaDependencyInjection.inject(_htmlSanitizer.HTMLSanitizer)(SanitizeHTMLValueConverter) || SanitizeHTMLValueConverter;
    SanitizeHTMLValueConverter = _aureliaBinding.valueConverter('sanitizeHTML')(SanitizeHTMLValueConverter) || SanitizeHTMLValueConverter;
    return SanitizeHTMLValueConverter;
  })();

  exports.SanitizeHTMLValueConverter = SanitizeHTMLValueConverter;
});