/* */ 
var baseIteratee = require('./_baseIteratee'),
    basePickBy = require('./_basePickBy');
function omitBy(object, predicate) {
  predicate = baseIteratee(predicate);
  return basePickBy(object, function(value, key) {
    return !predicate(value, key);
  });
}
module.exports = omitBy;
