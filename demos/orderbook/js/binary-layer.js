(function() {
// GET request that executes a callback with either an Float32Array
// containing bin values or null if no data exists
var getArrayBuffer = function( url, callback ) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', url, true);
  xhr.responseType = 'arraybuffer';
  xhr.onload = function(e) {
    if (this.status == 200) {
      callback(new Float64Array(this.response));
    } else {
      callback(null);
    }
  };
  xhr.send();
};


L.BinaryTileLayer = L.TileLayer.Canvas.extend({
  options: {
    async: true,
    tms: false,
    // unloadInvisibleTiles: true,
    noWrap: true,
    transform: 'log10',
    ramp: 'temperature'
  },

  initialize: function (url, options) {
    this._url = url;
    L.setOptions(this, options);
  },

  _loadTile: function (tile, tilePoint) {
    tile._layer = this;
    tile._tilePoint = tilePoint;

    this._adjustTilePoint(tilePoint);
    this._redrawTile(tile);

    if (!this.options.async) {
      this.tileDrawn(tile);
    }
  },

  _render: function(canvas, index, zoom, bins) {
    var options = this.options;
    var valueTransform = window.ValueTransformer[options.transform];
    var ramp = window.ColorRamp[options.ramp];
    var color = [];

    if (!bins) {
      // Exit early if no data
      this.tileDrawn(canvas);
      return;
    }
    var ctx = canvas.getContext("2d");
    var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    var data = imageData.data;
    var minMax = options.dataBounds[zoom];
    for (let i=0;i<bins.length;i++) {
      var value = valueTransform(minMax.min, minMax.max, 0, 90, bins[i]);

      color = ramp(value, color);
      data[i*4] = color[0];
      data[i*4+1] = color[1];
      data[i*4+2] = color[2];
      data[i*4+3] = color[3];
    }
    // Overwrite original image
    ctx.putImageData(imageData, 0, 0);
    this.tileDrawn(canvas);
  },

  drawTile: function(canvas, index, zoom) {
    var self = this;
    var url = this.getTileUrl(index);

    getArrayBuffer(url, function(bins) {
      self._render(canvas, index, zoom, bins);
    });
  }
});

}());