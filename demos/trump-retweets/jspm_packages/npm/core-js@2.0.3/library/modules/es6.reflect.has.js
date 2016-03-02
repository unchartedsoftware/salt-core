/* */ 
var $export = require('./_export');
$export($export.S, 'Reflect', {has: function has(target, propertyKey) {
    return propertyKey in target;
  }});
