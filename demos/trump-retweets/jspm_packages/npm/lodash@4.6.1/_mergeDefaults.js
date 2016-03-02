/* */ 
var baseMerge = require('./_baseMerge'),
    isObject = require('./isObject');
function mergeDefaults(objValue, srcValue, key, object, source, stack) {
  if (isObject(objValue) && isObject(srcValue)) {
    baseMerge(objValue, srcValue, undefined, mergeDefaults, stack.set(srcValue, objValue));
  }
  return objValue;
}
module.exports = mergeDefaults;
