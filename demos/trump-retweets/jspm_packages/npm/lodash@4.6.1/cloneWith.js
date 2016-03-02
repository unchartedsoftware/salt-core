/* */ 
var baseClone = require('./_baseClone');
function cloneWith(value, customizer) {
  return baseClone(value, false, true, customizer);
}
module.exports = cloneWith;
