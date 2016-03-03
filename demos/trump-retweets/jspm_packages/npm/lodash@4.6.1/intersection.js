/* */ 
var arrayMap = require('./_arrayMap'),
    baseCastArrayLikeObject = require('./_baseCastArrayLikeObject'),
    baseIntersection = require('./_baseIntersection'),
    rest = require('./rest');
var intersection = rest(function(arrays) {
  var mapped = arrayMap(arrays, baseCastArrayLikeObject);
  return (mapped.length && mapped[0] === arrays[0]) ? baseIntersection(mapped) : [];
});
module.exports = intersection;
