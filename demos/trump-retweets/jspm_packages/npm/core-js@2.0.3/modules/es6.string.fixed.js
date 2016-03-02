/* */ 
'use strict';
require('./_string-html')('fixed', function(createHTML) {
  return function fixed() {
    return createHTML(this, 'tt', '', '');
  };
});
