/* */ 
var arrayMap = require('./_arrayMap'),
    baseAt = require('./_baseAt'),
    baseFlatten = require('./_baseFlatten'),
    basePullAt = require('./_basePullAt'),
    compareAscending = require('./_compareAscending'),
    rest = require('./rest');
var pullAt = rest(function(array, indexes) {
  indexes = arrayMap(baseFlatten(indexes, 1), String);
  var result = baseAt(array, indexes);
  basePullAt(array, indexes.sort(compareAscending));
  return result;
});
module.exports = pullAt;
