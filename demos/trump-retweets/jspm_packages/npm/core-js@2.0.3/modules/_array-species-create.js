/* */ 
var isObject = require('./_is-object'),
    isArray = require('./_is-array'),
    SPECIES = require('./_wks')('species');
module.exports = function(original, length) {
  var C;
  if (isArray(original)) {
    C = original.constructor;
    if (typeof C == 'function' && (C === Array || isArray(C.prototype)))
      C = undefined;
    if (isObject(C)) {
      C = C[SPECIES];
      if (C === null)
        C = undefined;
    }
  }
  return new (C === undefined ? Array : C)(length);
};
