/* */ 
var $export = require('./_export');
$export($export.P + $export.R, 'Set', {toJSON: require('./_collection-to-json')('Set')});
