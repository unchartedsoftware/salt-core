/* */ 
var isObject = require('./_is-object'),
    floor = Math.floor;
module.exports = function isInteger(it) {
  return !isObject(it) && isFinite(it) && floor(it) === it;
};
