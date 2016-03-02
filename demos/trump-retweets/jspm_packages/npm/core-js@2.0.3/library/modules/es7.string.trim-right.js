/* */ 
'use strict';
require('./_string-trim')('trimRight', function($trim) {
  return function trimRight() {
    return $trim(this, 2);
  };
}, 'trimEnd');
