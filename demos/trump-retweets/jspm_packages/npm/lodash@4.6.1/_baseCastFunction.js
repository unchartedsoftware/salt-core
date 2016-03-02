/* */ 
var identity = require('./identity');
function baseCastFunction(value) {
  return typeof value == 'function' ? value : identity;
}
module.exports = baseCastFunction;
