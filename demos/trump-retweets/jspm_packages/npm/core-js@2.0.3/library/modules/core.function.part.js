/* */ 
var path = require('./_path'),
    $export = require('./_export');
require('./_core')._ = path._ = path._ || {};
$export($export.P + $export.F, 'Function', {part: require('./_partial')});
