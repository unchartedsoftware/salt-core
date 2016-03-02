/* */ 
'use strict';
var addToUnscopables = require('./_add-to-unscopables'),
    step = require('./_iter-step'),
    Iterators = require('./_iterators'),
    toIObject = require('./_to-iobject');
module.exports = require('./_iter-define')(Array, 'Array', function(iterated, kind) {
  this._t = toIObject(iterated);
  this._i = 0;
  this._k = kind;
}, function() {
  var O = this._t,
      kind = this._k,
      index = this._i++;
  if (!O || index >= O.length) {
    this._t = undefined;
    return step(1);
  }
  if (kind == 'keys')
    return step(0, index);
  if (kind == 'values')
    return step(0, O[index]);
  return step(0, [index, O[index]]);
}, 'values');
Iterators.Arguments = Iterators.Array;
addToUnscopables('keys');
addToUnscopables('values');
addToUnscopables('entries');
