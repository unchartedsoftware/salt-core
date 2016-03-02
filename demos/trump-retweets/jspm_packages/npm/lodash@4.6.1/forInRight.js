/* */ 
var baseCastFunction = require('./_baseCastFunction'),
    baseForRight = require('./_baseForRight'),
    keysIn = require('./keysIn');
function forInRight(object, iteratee) {
  return object == null ? object : baseForRight(object, baseCastFunction(iteratee), keysIn);
}
module.exports = forInRight;
