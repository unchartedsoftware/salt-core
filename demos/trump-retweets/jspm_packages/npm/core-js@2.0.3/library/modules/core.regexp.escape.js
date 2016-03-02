/* */ 
var $export = require('./_export'),
    $re = require('./_replacer')(/[\\^$*+?.()|[\]{}]/g, '\\$&');
$export($export.S, 'RegExp', {escape: function escape(it) {
    return $re(it);
  }});
