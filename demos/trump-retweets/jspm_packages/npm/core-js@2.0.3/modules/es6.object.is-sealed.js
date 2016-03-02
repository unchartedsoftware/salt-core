/* */ 
var isObject = require('./_is-object');
require('./_object-sap')('isSealed', function($isSealed) {
  return function isSealed(it) {
    return isObject(it) ? $isSealed ? $isSealed(it) : false : true;
  };
});
