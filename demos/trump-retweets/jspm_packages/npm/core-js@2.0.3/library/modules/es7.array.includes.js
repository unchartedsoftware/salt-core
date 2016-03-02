/* */ 
'use strict';
var $export = require('./_export'),
    $includes = require('./_array-includes')(true);
$export($export.P, 'Array', {includes: function includes(el) {
    return $includes(this, el, arguments.length > 1 ? arguments[1] : undefined);
  }});
require('./_add-to-unscopables')('includes');
