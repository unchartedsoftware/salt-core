/* */ 
var baseFlatten = require('./_baseFlatten');
var INFINITY = 1 / 0;
function flattenDeep(array) {
  var length = array ? array.length : 0;
  return length ? baseFlatten(array, INFINITY) : [];
}
module.exports = flattenDeep;
