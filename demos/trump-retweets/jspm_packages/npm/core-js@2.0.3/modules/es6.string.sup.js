/* */ 
'use strict';
require('./_string-html')('sup', function(createHTML) {
  return function sup() {
    return createHTML(this, 'sup', '', '');
  };
});
