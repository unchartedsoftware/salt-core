/* */ 
var baseCastFunction = require('./_baseCastFunction'),
    baseForOwnRight = require('./_baseForOwnRight');
function forOwnRight(object, iteratee) {
  return object && baseForOwnRight(object, baseCastFunction(iteratee));
}
module.exports = forOwnRight;
