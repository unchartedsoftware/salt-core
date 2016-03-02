/* */ 
var getLength = require('./_getLength'),
    isFunction = require('./isFunction'),
    isLength = require('./isLength');
function isArrayLike(value) {
  return value != null && isLength(getLength(value)) && !isFunction(value);
}
module.exports = isArrayLike;
