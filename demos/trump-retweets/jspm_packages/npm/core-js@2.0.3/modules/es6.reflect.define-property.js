/* */ 
var $ = require('./_'),
    $export = require('./_export'),
    anObject = require('./_an-object');
$export($export.S + $export.F * require('./_fails')(function() {
  Reflect.defineProperty($.setDesc({}, 1, {value: 1}), 1, {value: 2});
}), 'Reflect', {defineProperty: function defineProperty(target, propertyKey, attributes) {
    anObject(target);
    try {
      $.setDesc(target, propertyKey, attributes);
      return true;
    } catch (e) {
      return false;
    }
  }});
