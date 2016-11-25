window.ValueTransformer = {

  twoSidedLinear: function(min, max, rangeMin, rangeMax, value) {
    var maxMinusMin = max - min;
    var clampedMin = min + (rangeMin/100*(maxMinusMin));
    var clampedMax = min + (rangeMax/100*(maxMinusMin));
    var clampedValue = Math.min(Math.max(clampedMin, value), clampedMax);
    return (clampedValue/Math.max(Math.abs(clampedMax), Math.abs(clampedMin)) + 1)/2;
  },

  log10: function(min, max, rangeMin, rangeMax, value) {
    // Translate to log scale in [0,1]
    var result = Math.log(value-min + 1) / Math.log(max-min + 1);
    // Linear scale into given range min/max
    result = (result-(rangeMin/100)) / ((rangeMax-rangeMin)/100);
    // Bound to [0,1]
    return Math.min(Math.max(result, 0), 1);
  }
};