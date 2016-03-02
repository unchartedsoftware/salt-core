/* */ 
'use strict';
var ctx = require('./_ctx'),
    $export = require('./_export'),
    toObject = require('./_to-object'),
    call = require('./_iter-call'),
    isArrayIter = require('./_is-array-iter'),
    toLength = require('./_to-length'),
    getIterFn = require('./core.get-iterator-method');
$export($export.S + $export.F * !require('./_iter-detect')(function(iter) {
  Array.from(iter);
}), 'Array', {from: function from(arrayLike) {
    var O = toObject(arrayLike),
        C = typeof this == 'function' ? this : Array,
        aLen = arguments.length,
        mapfn = aLen > 1 ? arguments[1] : undefined,
        mapping = mapfn !== undefined,
        index = 0,
        iterFn = getIterFn(O),
        length,
        result,
        step,
        iterator;
    if (mapping)
      mapfn = ctx(mapfn, aLen > 2 ? arguments[2] : undefined, 2);
    if (iterFn != undefined && !(C == Array && isArrayIter(iterFn))) {
      for (iterator = iterFn.call(O), result = new C; !(step = iterator.next()).done; index++) {
        result[index] = mapping ? call(iterator, mapfn, [step.value, index], true) : step.value;
      }
    } else {
      length = toLength(O.length);
      for (result = new C(length); length > index; index++) {
        result[index] = mapping ? mapfn(O[index], index) : O[index];
      }
    }
    result.length = index;
    return result;
  }});
