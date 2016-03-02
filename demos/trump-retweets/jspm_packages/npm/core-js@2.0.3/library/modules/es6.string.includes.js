/* */ 
'use strict';
var $export = require('./_export'),
    context = require('./_string-context'),
    INCLUDES = 'includes';
$export($export.P + $export.F * require('./_fails-is-regexp')(INCLUDES), 'String', {includes: function includes(searchString) {
    return !!~context(this, searchString, INCLUDES).indexOf(searchString, arguments.length > 1 ? arguments[1] : undefined);
  }});
