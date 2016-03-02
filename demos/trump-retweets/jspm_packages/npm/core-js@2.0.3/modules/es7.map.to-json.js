/* */ 
var $export = require('./_export');
$export($export.P + $export.R, 'Map', {toJSON: require('./_collection-to-json')('Map')});
