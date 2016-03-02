/* */ 
var isArray = require('./isArray'),
    stringToPath = require('./_stringToPath');
function baseCastPath(value) {
  return isArray(value) ? value : stringToPath(value);
}
module.exports = baseCastPath;
