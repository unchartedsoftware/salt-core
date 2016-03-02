/* */ 
'use strict';
require('./_string-html')('blink', function(createHTML) {
  return function blink() {
    return createHTML(this, 'blink', '', '');
  };
});
