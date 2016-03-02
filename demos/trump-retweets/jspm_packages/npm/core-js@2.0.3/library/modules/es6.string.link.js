/* */ 
'use strict';
require('./_string-html')('link', function(createHTML) {
  return function link(url) {
    return createHTML(this, 'a', 'href', url);
  };
});
