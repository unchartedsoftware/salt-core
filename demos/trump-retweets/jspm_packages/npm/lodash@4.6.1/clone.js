/* */ 
var baseClone = require('./_baseClone');
function clone(value) {
  return baseClone(value, false, true);
}
module.exports = clone;
