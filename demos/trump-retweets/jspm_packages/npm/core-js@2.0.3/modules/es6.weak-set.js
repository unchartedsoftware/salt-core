/* */ 
'use strict';
var weak = require('./_collection-weak');
require('./_collection')('WeakSet', function(get) {
  return function WeakSet() {
    return get(this, arguments.length > 0 ? arguments[0] : undefined);
  };
}, {add: function add(value) {
    return weak.def(this, value, true);
  }}, weak, false, true);
