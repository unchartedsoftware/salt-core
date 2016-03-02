/* */ 
'use strict';
var $ = require('./_'),
    descriptor = require('./_property-desc'),
    setToStringTag = require('./_set-to-string-tag'),
    IteratorPrototype = {};
require('./_hide')(IteratorPrototype, require('./_wks')('iterator'), function() {
  return this;
});
module.exports = function(Constructor, NAME, next) {
  Constructor.prototype = $.create(IteratorPrototype, {next: descriptor(1, next)});
  setToStringTag(Constructor, NAME + ' Iterator');
};
