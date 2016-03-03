/* */ 
var baseCreate = require('./_baseCreate'),
    isPrototype = require('./_isPrototype');
var getPrototypeOf = Object.getPrototypeOf;
function initCloneObject(object) {
  return (typeof object.constructor == 'function' && !isPrototype(object)) ? baseCreate(getPrototypeOf(object)) : {};
}
module.exports = initCloneObject;
