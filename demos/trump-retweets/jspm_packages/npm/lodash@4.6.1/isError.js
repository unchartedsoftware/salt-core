/* */ 
var isObjectLike = require('./isObjectLike');
var errorTag = '[object Error]';
var objectProto = Object.prototype;
var objectToString = objectProto.toString;
function isError(value) {
  if (!isObjectLike(value)) {
    return false;
  }
  return (objectToString.call(value) == errorTag) || (typeof value.message == 'string' && typeof value.name == 'string');
}
module.exports = isError;
