/* */ 
var $export = require('./_export');
$export($export.P, 'Array', {fill: require('./_array-fill')});
require('./_add-to-unscopables')('fill');
