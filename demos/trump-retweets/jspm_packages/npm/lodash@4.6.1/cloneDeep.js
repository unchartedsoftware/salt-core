/* */ 
var baseClone = require('./_baseClone');
function cloneDeep(value) {
  return baseClone(value, true, true);
}
module.exports = cloneDeep;
