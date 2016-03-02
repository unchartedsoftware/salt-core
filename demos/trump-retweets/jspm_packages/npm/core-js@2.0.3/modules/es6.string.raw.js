/* */ 
var $export = require('./_export'),
    toIObject = require('./_to-iobject'),
    toLength = require('./_to-length');
$export($export.S, 'String', {raw: function raw(callSite) {
    var tpl = toIObject(callSite.raw),
        len = toLength(tpl.length),
        aLen = arguments.length,
        res = [],
        i = 0;
    while (len > i) {
      res.push(String(tpl[i++]));
      if (i < aLen)
        res.push(String(arguments[i]));
    }
    return res.join('');
  }});
