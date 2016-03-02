/* */ 
'use strict';
require('./_string-html')('big', function(createHTML) {
  return function big() {
    return createHTML(this, 'big', '', '');
  };
});
