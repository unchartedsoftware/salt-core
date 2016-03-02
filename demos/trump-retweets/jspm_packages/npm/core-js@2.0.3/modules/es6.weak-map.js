/* */ 
'use strict';
var each = require('./_').each,
    redefine = require('./_redefine'),
    meta = require('./_meta'),
    assign = require('./_object-assign'),
    weak = require('./_collection-weak'),
    isObject = require('./_is-object'),
    has = require('./_has'),
    getWeak = meta.getWeak,
    isExtensible = Object.isExtensible,
    uncaughtFrozenStore = weak.ufstore,
    tmp = {},
    InternalMap;
var wrapper = function(get) {
  return function WeakMap() {
    return get(this, arguments.length > 0 ? arguments[0] : undefined);
  };
};
var methods = {
  get: function get(key) {
    if (isObject(key)) {
      var data = getWeak(key);
      if (data === true)
        return uncaughtFrozenStore(this).get(key);
      return data ? data[this._i] : undefined;
    }
  },
  set: function set(key, value) {
    return weak.def(this, key, value);
  }
};
var $WeakMap = module.exports = require('./_collection')('WeakMap', wrapper, methods, weak, true, true);
if (new $WeakMap().set((Object.freeze || Object)(tmp), 7).get(tmp) != 7) {
  InternalMap = weak.getConstructor(wrapper);
  assign(InternalMap.prototype, methods);
  meta.NEED = true;
  each.call(['delete', 'has', 'get', 'set'], function(key) {
    var proto = $WeakMap.prototype,
        method = proto[key];
    redefine(proto, key, function(a, b) {
      if (isObject(a) && !isExtensible(a)) {
        if (!this._f)
          this._f = new InternalMap;
        var result = this._f[key](a, b);
        return key == 'set' ? this : result;
      }
      return method.call(this, a, b);
    });
  });
}
