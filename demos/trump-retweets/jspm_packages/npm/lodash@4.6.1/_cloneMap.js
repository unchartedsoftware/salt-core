/* */ 
var addMapEntry = require('./_addMapEntry'),
    arrayReduce = require('./_arrayReduce'),
    mapToArray = require('./_mapToArray');
function cloneMap(map) {
  return arrayReduce(mapToArray(map), addMapEntry, new map.constructor);
}
module.exports = cloneMap;
