/* */ 
var baseConvert = require('./_baseConvert');
function browserConvert(lodash, options) {
  return baseConvert(lodash, lodash, options);
}
if (typeof _ == 'function') {
  _ = browserConvert(_.runInContext());
}
module.exports = browserConvert;
