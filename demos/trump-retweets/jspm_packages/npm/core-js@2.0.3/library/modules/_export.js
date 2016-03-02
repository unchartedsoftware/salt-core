/* */ 
var global = require('./_global'),
    core = require('./_core'),
    ctx = require('./_ctx'),
    hide = require('./_hide'),
    PROTOTYPE = 'prototype';
var $export = function(type, name, source) {
  var IS_FORCED = type & $export.F,
      IS_GLOBAL = type & $export.G,
      IS_STATIC = type & $export.S,
      IS_PROTO = type & $export.P,
      IS_BIND = type & $export.B,
      IS_WRAP = type & $export.W,
      exports = IS_GLOBAL ? core : core[name] || (core[name] = {}),
      expProto = exports[PROTOTYPE],
      target = IS_GLOBAL ? global : IS_STATIC ? global[name] : (global[name] || {})[PROTOTYPE],
      key,
      own,
      out;
  if (IS_GLOBAL)
    source = name;
  for (key in source) {
    own = !IS_FORCED && target && target[key] !== undefined;
    if (own && key in exports)
      continue;
    out = own ? target[key] : source[key];
    exports[key] = IS_GLOBAL && typeof target[key] != 'function' ? source[key] : IS_BIND && own ? ctx(out, global) : IS_WRAP && target[key] == out ? (function(C) {
      var F = function(a, b, c) {
        if (this instanceof C) {
          switch (arguments.length) {
            case 0:
              return new C;
            case 1:
              return new C(a);
            case 2:
              return new C(a, b);
          }
          return new C(a, b, c);
        }
        return C.apply(this, arguments);
      };
      F[PROTOTYPE] = C[PROTOTYPE];
      return F;
    })(out) : IS_PROTO && typeof out == 'function' ? ctx(Function.call, out) : out;
    if (IS_PROTO) {
      (exports.virtual || (exports.virtual = {}))[key] = out;
      if (type & $export.R && expProto && !expProto[key])
        hide(expProto, key, out);
    }
  }
};
$export.F = 1;
$export.G = 2;
$export.S = 4;
$export.P = 8;
$export.B = 16;
$export.W = 32;
$export.U = 64;
$export.R = 128;
module.exports = $export;
