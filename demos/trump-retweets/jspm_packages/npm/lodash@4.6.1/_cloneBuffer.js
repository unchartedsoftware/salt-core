/* */ 
(function(Buffer) {
  function cloneBuffer(buffer, isDeep) {
    if (isDeep) {
      return buffer.slice();
    }
    var result = new buffer.constructor(buffer.length);
    buffer.copy(result);
    return result;
  }
  module.exports = cloneBuffer;
})(require('buffer').Buffer);
