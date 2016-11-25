(function() {

function rgb2lab(rgb) {
  var r = rgb[0] > 0.04045 ? Math.pow( ( rgb[0] + 0.055 ) / 1.055, 2.4) : rgb[0] / 12.92;
  var g = rgb[1] > 0.04045 ? Math.pow( ( rgb[1] + 0.055 ) / 1.055, 2.4) : rgb[1] / 12.92;
  var b = rgb[2] > 0.04045 ? Math.pow( ( rgb[2] + 0.055 ) / 1.055, 2.4) : rgb[2] / 12.92;

  //Observer. = 2°, Illuminant = D65
  var x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375;
  var y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750;
  var z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041;

  x = x / 0.95047;  // Observer= 2°, Illuminant= D65
  y = y / 1.00000;
  z = z / 1.08883;

  x = x > 0.008856 ? Math.pow(x, 1/3) : ( 7.787037 * x ) + ( 16 / 116 );
  y = y > 0.008856 ? Math.pow(y, 1/3) : ( 7.787037 * y ) + ( 16 / 116 );
  z = z > 0.008856 ? Math.pow(z, 1/3) : ( 7.787037 * z ) + ( 16 / 116 );

  return [( 116 * y ) - 16,
    500 * ( x - y ),
    200 * ( y - z ),
    rgb[3] ];
}

function lab2rgb(lab) {
  var y = ( lab[0] + 16 ) / 116;
  var x = y + lab[1] / 500;
  var z = y - lab[2] / 200;

  x = x > 0.206893034 ? x * x * x : (x - 4 / 29) / 7.787037;
  y = y > 0.206893034 ? y * y * y : (y - 4 / 29) / 7.787037;
  z = z > 0.206893034 ? z * z * z : (z - 4 / 29) / 7.787037;

  x = x * 0.95047;  // Observer= 2°, Illuminant= D65
  y = y * 1.00000;
  z = z * 1.08883;

  var r = x *  3.2404542 + y * -1.5371385 + z * -0.4985314;
  var g = x * -0.9692660 + y *  1.8760108 + z *  0.0415560;
  var b = x *  0.0556434 + y * -0.2040259 + z *  1.0572252;

  r = r > 0.00304 ? 1.055 * Math.pow(r, 1/2.4) - 0.055 : 12.92 * r;
  g = g > 0.00304 ? 1.055 * Math.pow(g, 1/2.4) - 0.055 : 12.92 * g;
  b = b > 0.00304 ? 1.055 * Math.pow(b, 1/2.4) - 0.055 : 12.92 * b;

  return [Math.max(Math.min(r, 1), 0), Math.max(Math.min(g, 1), 0), Math.max(Math.min(b, 1), 0), lab[3]];
}

function distance(c1, c2) {
  return Math.sqrt(
    (c1[0]-c2[0]) * (c1[0]-c2[0]) +
    (c1[1]-c2[1]) * (c1[1]-c2[1]) +
    (c1[2]-c2[2]) * (c1[2]-c2[2]) +
    (c1[3]-c2[3]) * (c1[3]-c2[3])
  );
}




var ColorRamp = {};

var GRADIENT_STEPS = 200;

// Linearly interpolate between a set of colors in RGB space
/*var buildGradientLookupTable = function(baseColors) {
  var outputGradient = [];
  for (var i=0;i<GRADIENT_STEPS;i++) {
    //find out which color indices in the base array we're between
    var left = Math.floor(i/GRADIENT_STEPS*(baseColors.length-1));
    var right = left+1;
    var stepProgress = (i/GRADIENT_STEPS - left/(baseColors.length-1)) / (1/(baseColors.length-1));
    //get scaled value between those colors based on i
    outputGradient.push([
      baseColors[left][0] + Math.floor(stepProgress*(baseColors[right][0]-baseColors[left][0])),
      baseColors[left][1] + Math.floor(stepProgress*(baseColors[right][1]-baseColors[left][1])),
      baseColors[left][2] + Math.floor(stepProgress*(baseColors[right][2]-baseColors[left][2])),
      baseColors[left][3] + Math.floor(stepProgress*(baseColors[right][3]-baseColors[left][3]))
    ]);
  }
  return outputGradient;
};*/

// Interpolate between a set of colors using even perceptual distance and interpolation in CIE L*a*b* space
var buildPerceptualLookupTable = function(baseColors) {
  var outputGradient = [];

  // Calculate perceptual spread in L*a*b* space
  var labs = _.map(baseColors, function(color) {
    return rgb2lab([color[0]/255, color[1]/255, color[2]/255, color[3]/255]);
  });
  var distances = _.map(labs, function(color, index, colors) {
    return index > 0 ? distance(color, colors[index-1]) : 0;
  });

  // Calculate cumulative distances in [0,1]
  var totalDistance = _.reduce(distances, function(a,b) { return a+b; }, 0);
  distances = _.map(distances, function(d) {
    return d / totalDistance;
  });

  var distanceTraversed = 0,
      key = 0,
      progress, stepProgress, rgb;

  for (var i=0;i<GRADIENT_STEPS;i++) {
    progress = i/(GRADIENT_STEPS-1);
    if (progress > distanceTraversed + distances[key+1] && key+1 < labs.length-1) {
      key += 1;
      distanceTraversed += distances[key];
    }
    stepProgress = (progress - distanceTraversed) / distances[key+1];

    rgb = lab2rgb([
      labs[key][0] + (labs[key+1][0] - labs[key][0]) * stepProgress,
      labs[key][1] + (labs[key+1][1] - labs[key][1]) * stepProgress,
      labs[key][2] + (labs[key+1][2] - labs[key][2]) * stepProgress,
      labs[key][3] + (labs[key+1][3] - labs[key][3]) * stepProgress
    ]);

    outputGradient.push([Math.round(rgb[0]*255), Math.round(rgb[1]*255), Math.round(rgb[2]*255), Math.round(rgb[3]*255)]);
  }

  outputGradient.unshift([0,0,0,0]);
  return outputGradient;
};



var COOL = buildPerceptualLookupTable([
  // [0x04,0x20,0x40,0x50],
  // [0x08,0x40,0x81,0x7f],
  [0x08,0x68,0xac,0xff],
  [0x2b,0x8c,0xbe,0xff],
  [0x4e,0xb3,0xd3,0xff],
  [0x7b,0xcc,0xc4,0xff],
  [0xa8,0xdd,0xb5,0xff],
  [0xcc,0xeb,0xc5,0xff],
  [0xe0,0xf3,0xdb,0xff],
  [0xf7,0xfc,0xf0,0xff]
]);

var HOT = buildPerceptualLookupTable([
  // [0x40,0x00,0x13,0x50],
  // [0x80,0x00,0x26,0x7f],
  [0xbd,0x00,0x26,0xff],
  [0xe3,0x1a,0x1c,0xff],
  [0xfc,0x4e,0x2a,0xff],
  [0xfd,0x8d,0x3c,0xff],
  [0xfe,0xb2,0x4c,0xff],
  [0xfe,0xd9,0x76,0xff],
  [0xff,0xed,0xa0,0xff]
]);

var FIRE = buildPerceptualLookupTable([
  [150, 0, 0, 150],
  [255, 255, 50, 255 ]
]);

var VERDANT = buildPerceptualLookupTable([
  [0x00, 0x40, 0x26, 0x50],
  [0x00, 0x5a, 0x32, 0x7f],
  [0x23, 0x84, 0x43, 0xff],
  [0x41, 0xab, 0x5d, 0xff],
  [0x78, 0xc6, 0x79, 0xff],
  [0xad, 0xdd, 0x8e, 0xff],
  [0xd9, 0xf0, 0xa3, 0xff],
  [0xf7, 0xfc, 0xb9, 0xff],
  [0xff, 0xff, 0xe5, 0xff]
]);

var SPECTRAL = buildPerceptualLookupTable([
  [0x26, 0x1A, 0x40, 0x50],
  [0x44, 0x2f, 0x72, 0x7f],
  [0xe1, 0x2b, 0x02, 0xff],
  [0x02, 0xdc, 0x01, 0xff],
  [0xff, 0xd2, 0x02, 0xff],
  [0xff, 0xff, 0xff, 0xff]
]);

var TEMPERATURE = buildPerceptualLookupTable([
  [0x00, 0x16, 0x40, 0x50],
  [0x00, 0x39, 0x66, 0x7f], //blue
  [0x31, 0x3d, 0x66, 0xff], //purple
  [0xe1, 0x2b, 0x02, 0xff], //red
  [0xff, 0xd2, 0x02, 0xff], //yellow
  [0xff, 0xff, 0xff, 0xff]  //white
]);

var GREYSCALE = buildPerceptualLookupTable([
  [0x00, 0x00, 0x00, 0x7f],
  [0x40, 0x40, 0x40, 0xff],
  [0xff, 0xff, 0xff, 0xff]
]);

var VALENCE_HOT = buildPerceptualLookupTable([
  // [0x40,0x00,0x13,0x50],
  [0x80,0x00,0x26,0x7f],
  [0xbd,0x00,0x26,0xff],
  [0xe3,0x1a,0x1c,0xff],
  [0xfc,0x4e,0x2a,0xff],
  [0xfd,0x8d,0x3c,0xff],
  // [0xfe,0xb2,0x4c,0xff],
  // [0xfe,0xd9,0x76,0xff],
  // [0xff,0xed,0xa0,0xff]
]);

var VALENCE_VERDANT = buildPerceptualLookupTable([
  [0x00, 0x40, 0x26, 0x50],
  [0x00, 0x5a, 0x32, 0x7f],
  [0x23, 0x84, 0x43, 0xff],
  [0x41, 0xab, 0x5d, 0xff],
  [0x78, 0xc6, 0x79, 0xff],
  // [0xad, 0xdd, 0x8e, 0xff],
  // [0xd9, 0xf0, 0xa3, 0xff],
  // [0xf7, 0xfc, 0xb9, 0xff],
  // [0xff, 0xff, 0xe5, 0xff]
]);


var buildLookupFunction = function(RAMP) {
  return function(scaledValue, inColor) {
    var color = RAMP[Math.floor(scaledValue*(GRADIENT_STEPS-1))];
    inColor[0] = color[0];
    inColor[1] = color[1];
    inColor[2] = color[2];
    inColor[3] = color[3];
    return inColor;
  };
};


/**
 * A cool color ramp using blues and opacity to represent values
 * @param scaledValue {Number} between 0 and 1
 * @param inColor {Array} A color value array in the form [R, G, B, A],
 *                        passed in for reuse. It will be overwritten with
 *                        the output color of this ramp.
 */
ColorRamp.cool = buildLookupFunction(COOL);
/**
 * A cool color ramp using reds and opacity to represent values
 * @param scaledValue {Number} between 0 and 1
 * @param inColor {Array} A color value array in the form [R, G, B, A],
 *                        passed in for reuse. It will be overwritten with
 *                        the output color of this ramp.
 */
ColorRamp.hot = buildLookupFunction(HOT);
ColorRamp.fire = buildLookupFunction(FIRE);
/**
 * A cool color ramp using greens and opacity to represent values
 * @param scaledValue {Number} between 0 and 1
 * @param inColor {Array} A color value array in the form [R, G, B, A],
 *                        passed in for reuse. It will be overwritten with
 *                        the output color of this ramp.
 */
ColorRamp.verdant = buildLookupFunction(VERDANT);
/**
 * A multi-color spectral color ramp using purple->red->green->yellow->white and opacity
 * to represent values. Adapted from Colin Ware's ramp
 * @param scaledValue {Number} between 0 and 1
 * @param inColor {Array} A color value array in the form [R, G, B, A],
 *                        passed in for reuse. It will be overwritten with
 *                        the output color of this ramp.
 */
ColorRamp.spectral = buildLookupFunction(SPECTRAL);

ColorRamp.temperature = buildLookupFunction(TEMPERATURE);

/**
 * A no-color color ramp using gray/white and opacity to represent values
 * @param scaledValue {Number} between 0 and 1
 * @param inColor {Array} A color value array in the form [R, G, B, A],
 *                        passed in for reuse. It will be overwritten with
 *                        the output color of this ramp.
 */
ColorRamp.grey = buildLookupFunction(GREYSCALE);

var valenceLow = buildLookupFunction(VALENCE_HOT);
var valenceHigh = buildLookupFunction(VALENCE_VERDANT);

ColorRamp.valence = function(scaledValue, inColor) {
  if (scaledValue > 0.5 ) {
    return valenceLow((scaledValue-0.5)*2, inColor);
  } else {
    return valenceHigh(scaledValue*2, inColor);
  }
};

//
ColorRamp.valence = function(scaledValue, inColor) {
  if (scaledValue > 0.5 ) {
    return valenceHigh((scaledValue-0.5)*2, inColor);
  } else if (scaledValue < 0.5 ) {
    return valenceLow((0.5-scaledValue)*2, inColor);
  } else {
    inColor[0] = 0;
    inColor[1] = 0;
    inColor[2] = 0;
    inColor[3] = 0;
    return inColor;
  }
};

window.ColorRamp = ColorRamp;

}());
