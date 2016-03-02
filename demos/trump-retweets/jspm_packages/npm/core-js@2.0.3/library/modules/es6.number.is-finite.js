/* */ 
var $export = require('./_export'),
    _isFinite = require('./_global').isFinite;
$export($export.S, 'Number', {isFinite: function isFinite(it) {
    return typeof it == 'number' && _isFinite(it);
  }});
