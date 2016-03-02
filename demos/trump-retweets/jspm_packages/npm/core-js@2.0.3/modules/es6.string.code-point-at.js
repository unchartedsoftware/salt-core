/* */ 
'use strict';
var $export = require('./_export'),
    $at = require('./_string-at')(false);
$export($export.P, 'String', {codePointAt: function codePointAt(pos) {
    return $at(this, pos);
  }});
