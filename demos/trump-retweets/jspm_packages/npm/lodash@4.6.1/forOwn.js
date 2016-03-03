/* */ 
var baseCastFunction = require('./_baseCastFunction'),
    baseForOwn = require('./_baseForOwn');
function forOwn(object, iteratee) {
  return object && baseForOwn(object, baseCastFunction(iteratee));
}
module.exports = forOwn;
