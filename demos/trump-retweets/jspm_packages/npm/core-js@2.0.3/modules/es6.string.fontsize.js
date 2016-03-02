/* */ 
'use strict';
require('./_string-html')('fontsize', function(createHTML) {
  return function fontsize(size) {
    return createHTML(this, 'font', 'size', size);
  };
});
