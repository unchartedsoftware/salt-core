/* */ 
var arrayMap = require('./_arrayMap'),
    baseCastArrayLikeObject = require('./_baseCastArrayLikeObject'),
    baseIntersection = require('./_baseIntersection'),
    last = require('./last'),
    rest = require('./rest');
var intersectionWith = rest(function(arrays) {
  var comparator = last(arrays),
      mapped = arrayMap(arrays, baseCastArrayLikeObject);
  if (comparator === last(mapped)) {
    comparator = undefined;
  } else {
    mapped.pop();
  }
  return (mapped.length && mapped[0] === arrays[0]) ? baseIntersection(mapped, undefined, comparator) : [];
});
module.exports = intersectionWith;
