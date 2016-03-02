/* */ 
'use strict';
require('./_string-html')('italics', function(createHTML) {
  return function italics() {
    return createHTML(this, 'i', '', '');
  };
});
