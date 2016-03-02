/* */ 
'use strict';
require('./_string-html')('small', function(createHTML) {
  return function small() {
    return createHTML(this, 'small', '', '');
  };
});
