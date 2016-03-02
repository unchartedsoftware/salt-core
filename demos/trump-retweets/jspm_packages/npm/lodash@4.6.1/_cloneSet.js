/* */ 
var addSetEntry = require('./_addSetEntry'),
    arrayReduce = require('./_arrayReduce'),
    setToArray = require('./_setToArray');
function cloneSet(set) {
  return arrayReduce(setToArray(set), addSetEntry, new set.constructor);
}
module.exports = cloneSet;
