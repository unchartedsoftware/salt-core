/* */ 
var arrayEachRight = require('./_arrayEachRight'),
    baseCastFunction = require('./_baseCastFunction'),
    baseEachRight = require('./_baseEachRight'),
    isArray = require('./isArray');
function forEachRight(collection, iteratee) {
  return (typeof iteratee == 'function' && isArray(collection)) ? arrayEachRight(collection, iteratee) : baseEachRight(collection, baseCastFunction(iteratee));
}
module.exports = forEachRight;
