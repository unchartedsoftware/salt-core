/* */ 
'use strict';
var toObject = require('./_to-object'),
    toIndex = require('./_to-index'),
    toLength = require('./_to-length');
module.exports = function fill(value) {
  var O = toObject(this),
      length = toLength(O.length),
      aLen = arguments.length,
      index = toIndex(aLen > 1 ? arguments[1] : undefined, length),
      end = aLen > 2 ? arguments[2] : undefined,
      endPos = end === undefined ? length : toIndex(end, length);
  while (endPos > index)
    O[index++] = value;
  return O;
};
