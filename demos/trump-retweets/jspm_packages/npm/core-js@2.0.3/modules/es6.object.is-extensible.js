/* */ 
var isObject = require('./_is-object');
require('./_object-sap')('isExtensible', function($isExtensible) {
  return function isExtensible(it) {
    return isObject(it) ? $isExtensible ? $isExtensible(it) : true : false;
  };
});
