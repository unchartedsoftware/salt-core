/* */ 
'use strict';
require('./_string-html')('fontcolor', function(createHTML) {
  return function fontcolor(color) {
    return createHTML(this, 'font', 'color', color);
  };
});
