/* */ 
var isObject = require('./_is-object'),
    document = require('./_global').document,
    is = isObject(document) && isObject(document.createElement);
module.exports = function(it) {
  return is ? document.createElement(it) : {};
};
