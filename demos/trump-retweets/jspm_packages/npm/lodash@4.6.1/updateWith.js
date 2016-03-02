/* */ 
var baseCastFunction = require('./_baseCastFunction'),
    baseUpdate = require('./_baseUpdate');
function updateWith(object, path, updater, customizer) {
  customizer = typeof customizer == 'function' ? customizer : undefined;
  return object == null ? object : baseUpdate(object, path, baseCastFunction(updater), customizer);
}
module.exports = updateWith;
