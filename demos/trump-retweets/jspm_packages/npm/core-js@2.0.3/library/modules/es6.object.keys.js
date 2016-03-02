/* */ 
var toObject = require('./_to-object');
require('./_object-sap')('keys', function($keys) {
  return function keys(it) {
    return $keys(toObject(it));
  };
});
