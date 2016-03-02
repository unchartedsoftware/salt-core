/* */ 
var $export = require('./_export');
$export($export.S, 'Number', {isNaN: function isNaN(number) {
    return number != number;
  }});
