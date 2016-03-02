/* */ 
var $ = require('./_');
if (require('./_descriptors') && /./g.flags != 'g')
  $.setDesc(RegExp.prototype, 'flags', {
    configurable: true,
    get: require('./_flags')
  });
