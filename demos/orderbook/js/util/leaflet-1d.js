window.Leaflet1D = {
  fixDrag: function(map) {

    // Monkeypatch drag handler to incorporate changes in this PR: https://github.com/Leaflet/Leaflet/pull/3510
    map.dragging.removeHooks();
    map.dragging._draggable = null;

    // Patch dragStart to calculate max drag amount in x/y
    map.dragging._onDragStart = function() {
      var map = this._map;

      if (map._panAnim) {
        map._panAnim.stop();
      }

      var bounds = L.latLngBounds(this._map.options.maxBounds);
      this._offsetLimit = L.bounds(
        this._map.latLngToContainerPoint(bounds.getNorthWest()).multiplyBy(-1),
        this._map.latLngToContainerPoint(bounds.getSouthEast()).multiplyBy(-1)
           .add(this._map.getSize())
      );

      map
          .fire('movestart')
          .fire('dragstart');

      if (map.options.inertia) {
        this._positions = [];
        this._times = [];
      }
    };

    // Monkeypatch draggable to fix, see below
    L.Draggable.prototype._onMove = function (e) {
      if (e.touches && e.touches.length > 1) {
        this._moved = true;
        return;
      }

      var first = (e.touches && e.touches.length === 1 ? e.touches[0] : e),
          newPoint = new L.Point(first.clientX, first.clientY),
          offset = newPoint.subtract(this._startPoint);

      if (!offset.x && !offset.y) { return; }
      if (L.Browser.touch && Math.abs(offset.x) + Math.abs(offset.y) < 3) { return; }

      L.DomEvent.preventDefault(e);

      if (!this._moved) {
        this.fire('dragstart');

        this._moved = true;
        // Exact dupe of _onMove without this line:
        // this._startPos = L.DomUtil.getPosition(this._element).subtract(offset);
        // Allowing the first move event to update the startPos allows the drag to move outside of map bounds

        L.DomUtil.addClass(document.body, 'leaflet-dragging');
        this._lastTarget = e.target || e.srcElement;
        L.DomUtil.addClass(this._lastTarget, 'leaflet-drag-target');
      }

      this._newPos = this._startPos.add(offset);
      this._moving = true;

      L.Util.cancelAnimFrame(this._animRequest);
      this._animRequest = L.Util.requestAnimFrame(this._updatePosition, this, true, this._dragStartTarget);
    };

    // Patch predrag to adjust _newPos based on limits
    map.dragging._onPreDrag = function(e) {
      var offset = this._draggable._newPos.subtract(this._draggable._startPos);
      offset.y = 0;

      var limit = this._offsetLimit;
      if (offset.x < limit.min.x) { offset.x = limit.min.x; }
      if (offset.x > limit.max.x) { offset.x = limit.max.x; }

      this._draggable._newPos = this._draggable._startPos.add(offset);
    };

    // Re-add draggable now that it's patched
    map.dragging.addHooks();

    // Have to force register predrag (rather than patch the init code to do so)
    map.dragging._draggable.on('predrag', map.dragging._onPreDrag, map.dragging);
  },

  fixZoom: function(map) {
    // Ensure when zooming (and on initial view set) the map always stays aligned to the bottom of the viewport
    map.setView = function (center, zoom) {
      var fixedBottom = -85.05113;

      zoom = zoom === undefined ? this.getZoom() : zoom;
      zoom = this._limitZoom(zoom);

      var bottomLatLng = L.latLng(center);
      bottomLatLng.lat = fixedBottom;
      var bottomPixel = this.project(bottomLatLng, zoom);

      var size = this.getSize();
      var midPixel = bottomPixel.clone();
      midPixel.y -= size.y / 2;
      var newMidLatLng = this._limitCenter(this.unproject(midPixel, zoom), zoom, this.options.maxBounds);

      if (this._panAnim) {
        this._panAnim.stop();
      }
      if (this._loaded && !this.options.reset && this.options !== true) {
        if (this.options.animate !== undefined) {
          this.options.zoom = L.extend({animate: this.options.animate}, this.options.zoom);
          this.options.pan = L.extend({animate: this.options.animate}, this.options.pan);
        }

        // try animating pan or zoom
        var animated = (this._zoom !== zoom) ?
          this._tryAnimatedZoom && this._tryAnimatedZoom(newMidLatLng, zoom, this.options.zoom) :
          this._tryAnimatedPan(newMidLatLng, this.options.pan);

        if (animated) {
          // prevent resize handler call, the view will refresh after animation anyway
          clearTimeout(this._sizeTimer);
          return this;
        }
      }

      this._resetView(newMidLatLng, zoom);
      return this;
    };
  }
};
