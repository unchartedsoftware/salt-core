/* */ 
var isArrayLikeObject = require('./isArrayLikeObject');
function baseCastArrayLikeObject(value) {
  return isArrayLikeObject(value) ? value : [];
}
module.exports = baseCastArrayLikeObject;
