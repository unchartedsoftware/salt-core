/* */ 
var baseCastFunction = require('./_baseCastFunction'),
    baseUpdate = require('./_baseUpdate');
function update(object, path, updater) {
  return object == null ? object : baseUpdate(object, path, baseCastFunction(updater));
}
module.exports = update;
