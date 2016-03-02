/* */ 
'use strict';
require('./_string-html')('strike', function(createHTML) {
  return function strike() {
    return createHTML(this, 'strike', '', '');
  };
});
