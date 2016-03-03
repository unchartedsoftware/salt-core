/* */ 
var baseFlatten = require('./_baseFlatten'),
    toInteger = require('./toInteger');
function flattenDepth(array, depth) {
  var length = array ? array.length : 0;
  if (!length) {
    return [];
  }
  depth = depth === undefined ? 1 : toInteger(depth);
  return baseFlatten(array, depth);
}
module.exports = flattenDepth;
