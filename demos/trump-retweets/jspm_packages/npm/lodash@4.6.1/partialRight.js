/* */ 
var createWrapper = require('./_createWrapper'),
    getPlaceholder = require('./_getPlaceholder'),
    replaceHolders = require('./_replaceHolders'),
    rest = require('./rest');
var PARTIAL_RIGHT_FLAG = 64;
var partialRight = rest(function(func, partials) {
  var holders = replaceHolders(partials, getPlaceholder(partialRight));
  return createWrapper(func, PARTIAL_RIGHT_FLAG, undefined, partials, holders);
});
partialRight.placeholder = {};
module.exports = partialRight;
