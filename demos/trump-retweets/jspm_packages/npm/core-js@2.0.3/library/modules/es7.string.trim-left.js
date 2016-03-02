/* */ 
'use strict';
require('./_string-trim')('trimLeft', function($trim) {
  return function trimLeft() {
    return $trim(this, 1);
  };
}, 'trimStart');
