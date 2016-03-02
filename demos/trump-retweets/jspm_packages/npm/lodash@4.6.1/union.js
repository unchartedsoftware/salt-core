/* */ 
var baseFlatten = require('./_baseFlatten'),
    baseUniq = require('./_baseUniq'),
    rest = require('./rest');
var union = rest(function(arrays) {
  return baseUniq(baseFlatten(arrays, 1, true));
});
module.exports = union;
