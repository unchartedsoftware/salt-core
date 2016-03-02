/* */ 
'use strict';
var $ = require('./_'),
    hide = require('./_hide'),
    redefineAll = require('./_redefine-all'),
    ctx = require('./_ctx'),
    anInstance = require('./_an-instance'),
    defined = require('./_defined'),
    forOf = require('./_for-of'),
    $iterDefine = require('./_iter-define'),
    step = require('./_iter-step'),
    setSpecies = require('./_set-species'),
    DESCRIPTORS = require('./_descriptors'),
    fastKey = require('./_meta').fastKey,
    SIZE = DESCRIPTORS ? '_s' : 'size';
var getEntry = function(that, key) {
  var index = fastKey(key),
      entry;
  if (index !== 'F')
    return that._i[index];
  for (entry = that._f; entry; entry = entry.n) {
    if (entry.k == key)
      return entry;
  }
};
module.exports = {
  getConstructor: function(wrapper, NAME, IS_MAP, ADDER) {
    var C = wrapper(function(that, iterable) {
      anInstance(that, C, NAME, '_i');
      that._i = $.create(null);
      that._f = undefined;
      that._l = undefined;
      that[SIZE] = 0;
      if (iterable != undefined)
        forOf(iterable, IS_MAP, that[ADDER], that);
    });
    redefineAll(C.prototype, {
      clear: function clear() {
        for (var that = this,
            data = that._i,
            entry = that._f; entry; entry = entry.n) {
          entry.r = true;
          if (entry.p)
            entry.p = entry.p.n = undefined;
          delete data[entry.i];
        }
        that._f = that._l = undefined;
        that[SIZE] = 0;
      },
      'delete': function(key) {
        var that = this,
            entry = getEntry(that, key);
        if (entry) {
          var next = entry.n,
              prev = entry.p;
          delete that._i[entry.i];
          entry.r = true;
          if (prev)
            prev.n = next;
          if (next)
            next.p = prev;
          if (that._f == entry)
            that._f = next;
          if (that._l == entry)
            that._l = prev;
          that[SIZE]--;
        }
        return !!entry;
      },
      forEach: function forEach(callbackfn) {
        anInstance(this, C, 'forEach');
        var f = ctx(callbackfn, arguments.length > 1 ? arguments[1] : undefined, 3),
            entry;
        while (entry = entry ? entry.n : this._f) {
          f(entry.v, entry.k, this);
          while (entry && entry.r)
            entry = entry.p;
        }
      },
      has: function has(key) {
        return !!getEntry(this, key);
      }
    });
    if (DESCRIPTORS)
      $.setDesc(C.prototype, 'size', {get: function() {
          return defined(this[SIZE]);
        }});
    return C;
  },
  def: function(that, key, value) {
    var entry = getEntry(that, key),
        prev,
        index;
    if (entry) {
      entry.v = value;
    } else {
      that._l = entry = {
        i: index = fastKey(key, true),
        k: key,
        v: value,
        p: prev = that._l,
        n: undefined,
        r: false
      };
      if (!that._f)
        that._f = entry;
      if (prev)
        prev.n = entry;
      that[SIZE]++;
      if (index !== 'F')
        that._i[index] = entry;
    }
    return that;
  },
  getEntry: getEntry,
  setStrong: function(C, NAME, IS_MAP) {
    $iterDefine(C, NAME, function(iterated, kind) {
      this._t = iterated;
      this._k = kind;
      this._l = undefined;
    }, function() {
      var that = this,
          kind = that._k,
          entry = that._l;
      while (entry && entry.r)
        entry = entry.p;
      if (!that._t || !(that._l = entry = entry ? entry.n : that._t._f)) {
        that._t = undefined;
        return step(1);
      }
      if (kind == 'keys')
        return step(0, entry.k);
      if (kind == 'values')
        return step(0, entry.v);
      return step(0, [entry.k, entry.v]);
    }, IS_MAP ? 'entries' : 'values', !IS_MAP, true);
    setSpecies(NAME);
  }
};
