/* */ 
var baseCastFunction = require('./_baseCastFunction'),
    baseFor = require('./_baseFor'),
    keysIn = require('./keysIn');
function forIn(object, iteratee) {
  return object == null ? object : baseFor(object, baseCastFunction(iteratee), keysIn);
}
module.exports = forIn;
