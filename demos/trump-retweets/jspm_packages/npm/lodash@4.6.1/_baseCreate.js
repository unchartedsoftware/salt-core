/* */ 
var isObject = require('./isObject');
var objectCreate = Object.create;
function baseCreate(proto) {
  return isObject(proto) ? objectCreate(proto) : {};
}
module.exports = baseCreate;
