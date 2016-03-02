/* */ 
var META = require('./_uid')('meta'),
    isObject = require('./_is-object'),
    has = require('./_has'),
    setDesc = require('./_').setDesc,
    id = 0;
var isExtensible = Object.isExtensible || function() {
  return true;
};
var FREEZE = !require('./_fails')(function() {
  return isExtensible(Object.preventExtensions({}));
});
var setMeta = function(it) {
  setDesc(it, META, {value: {
      i: 'O' + ++id,
      w: {}
    }});
};
var fastKey = function(it, create) {
  if (!isObject(it))
    return typeof it == 'symbol' ? it : (typeof it == 'string' ? 'S' : 'P') + it;
  if (!has(it, META)) {
    if (!isExtensible(it))
      return 'F';
    if (!create)
      return 'E';
    setMeta(it);
  }
  return it[META].i;
};
var getWeak = function(it, create) {
  if (!has(it, META)) {
    if (!isExtensible(it))
      return true;
    if (!create)
      return false;
    setMeta(it);
  }
  return it[META].w;
};
var onFreeze = function(it) {
  if (FREEZE && meta.NEED && isExtensible(it) && !has(it, META))
    setMeta(it);
  return it;
};
var meta = module.exports = {
  KEY: META,
  NEED: false,
  fastKey: fastKey,
  getWeak: getWeak,
  onFreeze: onFreeze
};
