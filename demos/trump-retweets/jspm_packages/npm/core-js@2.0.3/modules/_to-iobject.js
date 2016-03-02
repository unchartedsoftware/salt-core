/* */ 
var IObject = require('./_iobject'),
    defined = require('./_defined');
module.exports = function(it) {
  return IObject(defined(it));
};
