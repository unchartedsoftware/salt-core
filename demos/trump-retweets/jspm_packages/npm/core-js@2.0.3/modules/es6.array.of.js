/* */ 
'use strict';
var $export = require('./_export');
$export($export.S + $export.F * require('./_fails')(function() {
  function F() {}
  return !(Array.of.call(F) instanceof F);
}), 'Array', {of: function of() {
    var index = 0,
        aLen = arguments.length,
        result = new (typeof this == 'function' ? this : Array)(aLen);
    while (aLen > index)
      result[index] = arguments[index++];
    result.length = aLen;
    return result;
  }});
