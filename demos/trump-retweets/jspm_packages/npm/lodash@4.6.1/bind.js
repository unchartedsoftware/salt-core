/* */ 
var createWrapper = require('./_createWrapper'),
    getPlaceholder = require('./_getPlaceholder'),
    replaceHolders = require('./_replaceHolders'),
    rest = require('./rest');
var BIND_FLAG = 1,
    PARTIAL_FLAG = 32;
var bind = rest(function(func, thisArg, partials) {
  var bitmask = BIND_FLAG;
  if (partials.length) {
    var holders = replaceHolders(partials, getPlaceholder(bind));
    bitmask |= PARTIAL_FLAG;
  }
  return createWrapper(func, bitmask, thisArg, partials, holders);
});
bind.placeholder = {};
module.exports = bind;
