/* */ 
var $export = require('./_export');
$export($export.P, 'Array', {copyWithin: require('./_array-copy-within')});
require('./_add-to-unscopables')('copyWithin');
