/* */ 
var toIObject = require('./_to-iobject');
require('./_object-sap')('getOwnPropertyDescriptor', function($getOwnPropertyDescriptor) {
  return function getOwnPropertyDescriptor(it, key) {
    return $getOwnPropertyDescriptor(toIObject(it), key);
  };
});
