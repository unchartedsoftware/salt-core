/* */ 
'use strict';
require('./_string-html')('anchor', function(createHTML) {
  return function anchor(name) {
    return createHTML(this, 'a', 'name', name);
  };
});
