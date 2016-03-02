/* */ 
'use strict';
require('./_string-html')('bold', function(createHTML) {
  return function bold() {
    return createHTML(this, 'b', '', '');
  };
});
