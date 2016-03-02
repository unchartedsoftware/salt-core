/* */ 
'use strict';
require('./_string-html')('sub', function(createHTML) {
  return function sub() {
    return createHTML(this, 'sub', '', '');
  };
});
