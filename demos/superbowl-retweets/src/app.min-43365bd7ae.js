"bundle";!function(){var a=System.get("@@amd-helpers").createDefine();define("app.html!github:systemjs/plugin-text@0.0.3.js",[],function(){return'<template> <require from="./components/map"></require> <require from="./components/tweet-list"></require> <require from="./components/twitter-card"></require> <require from="./converters/date-format"></require> <div class="viz-container"> <map tweet-promise.bind="tweetList()" density-layer.bind="densityLayer" hit-layer.bind="hitLayer" selected-id.bind="selectedId" move-to-timestamp.bind="moveToTimestamp" bounds.bind="bounds" select.trigger="onSelect($event)" offscreen.trigger="onOffscreen($event)"></map> <twitter-card class="${selectedId ? \'tweet-selected\' : \'\'}" tweet-id.bind="selectedId" offscreen.bind="offscreen"></twitter-card> </div> <div class="text-container ${selectedId ? \'tweet-selected\' : \'\'}"> <div class="readable"> <h1>Superbowl <em>50</em></h1> <p show.bind="!nodata"> These are the <em>most retweeted</em> tweets mentioning the Superbowl, the Broncos, or the Panthers from 2:30pm to 11pm Eastern on Superbowl Sunday. </p> <p show.bind="!nodata"> Pick a tweet below to see it rise in <em>time &#8680;</em> and <em>&#8679; total retweets</em>. </p> <p show.bind="nodata"> We\'re fetching the latest data right now.<br> Check back soon! </p> <tweet-list show.bind="!nodata" tweet-promise.bind="tweetList()" selected-id.bind="selectedId" select.trigger="onSelect($event)" moveto.trigger="onMoveTo($event)"></tweet-list> </div> </div> <nav class="main ${selectedId ? \'tweet-selected\' : \'\'}"> <a href="http://blog.uncharted.software/2016/03/twitter-talks-trump/" target="_blank"><img src="imgs/uncharted-logo.png"></a> <span class="last-updated" show.bind="!nodata">Last updated: ${bounds.x.max | dateFormat}</span> <a class="back-button" href="#" click.trigger="clear()">&times;</a> <hr> </nav> </template>'}),a()}(),System.register("services/tile-layer.js",["aurelia-fetch-client","fetch"],function(a){"use strict";var b,c;return{setters:[function(a){b=a.HttpClient},function(a){}],execute:function(){c=function(){function a(c,d){babelHelpers.classCallCheck(this,a),this.baseUrl=c,this.ext=d,this.http=new b,this.http.configure(function(a){a.useStandardConfiguration().withBaseUrl(c)})}return babelHelpers.createClass(a,[{key:"metadata",value:function(){return this.metadataPromise||(this.metadataPromise=this.http.fetch("/metadata.json").then(function(a){return a.json()})),this.metadataPromise}},{key:"tileUrl",value:function(){return this.baseUrl+"/{z}/{x}/{y}."+this.ext}}]),a}(),a("TileLayer",c)}}}),System.register("app.js",["aurelia-fetch-client","./services/tile-layer"],function(a){"use strict";var b,c,d;return{setters:[function(a){b=a.HttpClient},function(a){c=a.TileLayer}],execute:function(){d=function(){function a(){var d=this;babelHelpers.classCallCheck(this,a),this.densityLayer=new c("//s3.amazonaws.com/embed.pantera.io/superbowl-retweets/retweets","bin"),this.hitLayer=new c("//s3.amazonaws.com/embed.pantera.io/superbowl-retweets/retweets-top200","json"),this.selectedId=null,this.moveToTimestamp=null,this.bounds=null;var e=new b;e.configure(function(a){a.useStandardConfiguration().withBaseUrl("//s3.amazonaws.com/embed.pantera.io/superbowl-retweets/retweets")}),e.fetch("/stats.json").then(function(a){return a.json()}).then(function(a){d.bounds=a})["catch"](function(){d.nodata=!0})}return babelHelpers.createClass(a,[{key:"onSelect",value:function(a){this.offscreen=void 0,this.selectedId=a.detail.value,this.selectedId?window.location.hash="#"+this.selectedId:this.clear()}},{key:"onMoveTo",value:function(a){this.moveToTimestamp=a.detail.value}},{key:"onOffscreen",value:function(a){this.offscreen=a.detail.value,console.log(this.offscreen)}},{key:"clear",value:function(a){this.selectedId=void 0,this.moveToTimestamp=void 0,this.offscreen=void 0,window.location.hash="#"}},{key:"tweetList",value:function(){return this.hitLayer.metadata()}},{key:"activate",value:function(){if(window.location.hash){var a=window.location.hash.substr(1);this.selectedId=a}}}]),a}(),a("App",d)}}}),function(){var a=System.get("@@amd-helpers").createDefine();define("components/map.html!github:systemjs/plugin-text@0.0.3.js",[],function(){return'<template> <div ref="mapContainer" class="map-container"></div> <div ref="xPosition" class="x-position"> <span class="line"></span> <span class="label"></span> </div> <div ref="yPosition" class="y-position"> <span class="line"></span> <span class="label"></span> </div> </template>'}),a()}(),System.register("util/leaflet-1d.js",[],function(a){"use strict";function b(a){a.dragging.removeHooks(),a.dragging._draggable=null,a.dragging._onDragStart=function(){var a=this._map;a._panAnim&&a._panAnim.stop();var b=L.latLngBounds(this._map.options.maxBounds);this._offsetLimit=L.bounds(this._map.latLngToContainerPoint(b.getNorthWest()).multiplyBy(-1),this._map.latLngToContainerPoint(b.getSouthEast()).multiplyBy(-1).add(this._map.getSize())),a.fire("movestart").fire("dragstart"),a.options.inertia&&(this._positions=[],this._times=[])},L.Draggable.prototype._onMove=function(a){if(a.touches&&a.touches.length>1)return void(this._moved=!0);var b=a.touches&&1===a.touches.length?a.touches[0]:a,c=new L.Point(b.clientX,b.clientY),d=c.subtract(this._startPoint);(d.x||d.y)&&(L.Browser.touch&&Math.abs(d.x)+Math.abs(d.y)<3||(L.DomEvent.preventDefault(a),this._moved||(this.fire("dragstart"),this._moved=!0,L.DomUtil.addClass(document.body,"leaflet-dragging"),this._lastTarget=a.target||a.srcElement,L.DomUtil.addClass(this._lastTarget,"leaflet-drag-target")),this._newPos=this._startPos.add(d),this._moving=!0,L.Util.cancelAnimFrame(this._animRequest),this._animRequest=L.Util.requestAnimFrame(this._updatePosition,this,!0,this._dragStartTarget)))},a.dragging._onPreDrag=function(a){var b=this._draggable._newPos.subtract(this._draggable._startPos);b.y=0;var c=this._offsetLimit;b.x<c.min.x&&(b.x=c.min.x),b.x>c.max.x&&(b.x=c.max.x),this._draggable._newPos=this._draggable._startPos.add(b)},a.dragging.addHooks(),a.dragging._draggable.on("predrag",a.dragging._onPreDrag,a.dragging)}function c(a){a.setView=function(a,b){var c=-85.05113;b=void 0===b?this.getZoom():b,b=this._limitZoom(b);var d=L.latLng(a);d.lat=c;var e=this.project(d,b),f=this.getSize(),g=e.clone();g.y-=f.y/2;var h=this._limitCenter(this.unproject(g,b),b,this.options.maxBounds);if(this._panAnim&&this._panAnim.stop(),this._loaded&&!this.options.reset&&this.options!==!0){void 0!==this.options.animate&&(this.options.zoom=L.extend({animate:this.options.animate},this.options.zoom),this.options.pan=L.extend({animate:this.options.animate},this.options.pan));var i=this._zoom!==b?this._tryAnimatedZoom&&this._tryAnimatedZoom(h,b,this.options.zoom):this._tryAnimatedPan(h,this.options.pan);if(i)return clearTimeout(this._sizeTimer),this}return this._resetView(h,b),this}}return a("fixDrag",b),a("fixZoom",c),{setters:[],execute:function(){}}}),System.register("util/value-transform.js",[],function(a){"use strict";var b;return{setters:[],execute:function(){b={twoSidedLinear:function(a,b,c,d,e){var f=b-a,g=a+c/100*f,h=a+d/100*f,i=Math.min(Math.max(g,e),h);return(i/Math.max(Math.abs(h),Math.abs(g))+1)/2},log10:function(a,b,c,d,e){var f=Math.log(e-a+1)/Math.log(b-a+1);return f=(f-c/100)/((d-c)/100),Math.min(Math.max(f,0),1)}},a("ValueTransformer",b)}}}),System.register("components/binary-layer.js",["leaflet","lodash","../util/value-transform","../util/color-ramp"],function(a){"use strict";var b,c,d,e,f,g;return{setters:[function(a){b=a["default"]},function(a){c=a["default"]},function(a){d=a.ValueTransformer},function(a){e=a.ColorRamp}],execute:function(){f=function(a,b){var c=new XMLHttpRequest;c.open("GET",a,!0),c.responseType="arraybuffer",c.onload=function(a){b(200==this.status?new Float64Array(this.response):null)},c.send()},g=b.TileLayer.Canvas.extend({options:{async:!0,tms:!0,noWrap:!0,transform:"log10",ramp:"temperature"},initialize:function(a,c){this._url=a,b.setOptions(this,c)},_loadTile:function(a,b){a._layer=this,a._tilePoint=b,this._adjustTilePoint(b),this._redrawTile(a),this.options.async||this.tileDrawn(a)},_render:function(a,b,c,f){var g=this.options,h=d[g.transform],i=e[g.ramp],j=[];if(!f)return void this.tileDrawn(a);for(var k=a.getContext("2d"),l=k.getImageData(0,0,a.width,a.height),m=l.data,n=g.dataBounds[c],o=0;o<f.length;o++){var p=h(n.min,n.max,0,100,f[o]);j=i(p,j),m[4*o]=j[0],m[4*o+1]=j[1],m[4*o+2]=j[2],m[4*o+3]=j[3]}k.putImageData(l,0,0),this.tileDrawn(a)},drawTile:function(a,b,c){var d=this,e=this.getTileUrl(b);f(e,function(e){d._render(a,b,c,e)})}}),a("BinaryTileLayer",g)}}}),System.register("util/color-ramp.js",[],function(a){"use strict";function b(a){var b=a[0]>.04045?Math.pow((a[0]+.055)/1.055,2.4):a[0]/12.92,c=a[1]>.04045?Math.pow((a[1]+.055)/1.055,2.4):a[1]/12.92,d=a[2]>.04045?Math.pow((a[2]+.055)/1.055,2.4):a[2]/12.92,e=.4124564*b+.3575761*c+.1804375*d,f=.2126729*b+.7151522*c+.072175*d,g=.0193339*b+.119192*c+.9503041*d;return e/=.95047,f/=1,g/=1.08883,e=e>.008856?Math.pow(e,1/3):7.787037*e+16/116,f=f>.008856?Math.pow(f,1/3):7.787037*f+16/116,g=g>.008856?Math.pow(g,1/3):7.787037*g+16/116,[116*f-16,500*(e-f),200*(f-g),a[3]]}function c(a){var b=(a[0]+16)/116,c=b+a[1]/500,d=b-a[2]/200;c=c>.206893034?c*c*c:(c-4/29)/7.787037,b=b>.206893034?b*b*b:(b-4/29)/7.787037,d=d>.206893034?d*d*d:(d-4/29)/7.787037,c=.95047*c,b=1*b,d=1.08883*d;var e=3.2404542*c+-1.5371385*b+d*-.4985314,f=c*-.969266+1.8760108*b+.041556*d,g=.0556434*c+b*-.2040259+1.0572252*d;return e=e>.00304?1.055*Math.pow(e,1/2.4)-.055:12.92*e,f=f>.00304?1.055*Math.pow(f,1/2.4)-.055:12.92*f,g=g>.00304?1.055*Math.pow(g,1/2.4)-.055:12.92*g,[Math.max(Math.min(e,1),0),Math.max(Math.min(f,1),0),Math.max(Math.min(g,1),0),a[3]]}function d(a,b){return Math.sqrt((a[0]-b[0])*(a[0]-b[0])+(a[1]-b[1])*(a[1]-b[1])+(a[2]-b[2])*(a[2]-b[2])+(a[3]-b[3])*(a[3]-b[3]))}var e,f,g,h,i,j,k,l,m,n,o,p,q,r,s;return{setters:[],execute:function(){e={},f=200,g=function(a){var e=[],g=_.map(a,function(a){return b([a[0]/255,a[1]/255,a[2]/255,a[3]/255])}),h=_.map(g,function(a,b,c){return b>0?d(a,c[b-1]):0}),i=_.reduce(h,function(a,b){return a+b},0);h=_.map(h,function(a){return a/i});for(var j,k,l,m=0,n=0,o=0;f>o;o++)j=o/(f-1),j>m+h[n+1]&&n+1<g.length-1&&(n+=1,m+=h[n]),k=(j-m)/h[n+1],l=c([g[n][0]+(g[n+1][0]-g[n][0])*k,g[n][1]+(g[n+1][1]-g[n][1])*k,g[n][2]+(g[n+1][2]-g[n][2])*k,g[n][3]+(g[n+1][3]-g[n][3])*k]),e.push([Math.round(255*l[0]),Math.round(255*l[1]),Math.round(255*l[2]),Math.round(255*l[3])]);return e.unshift([0,0,0,0]),e},h=g([[8,104,172,255],[43,140,190,255],[78,179,211,255],[123,204,196,255],[168,221,181,255],[204,235,197,255],[224,243,219,255],[247,252,240,255]]),i=g([[189,0,38,255],[227,26,28,255],[252,78,42,255],[253,141,60,255],[254,178,76,255],[254,217,118,255],[255,237,160,255]]),j=g([[150,0,0,150],[255,255,50,255]]),k=g([[0,64,38,80],[0,90,50,127],[35,132,67,255],[65,171,93,255],[120,198,121,255],[173,221,142,255],[217,240,163,255],[247,252,185,255],[255,255,229,255]]),l=g([[38,26,64,80],[68,47,114,127],[225,43,2,255],[2,220,1,255],[255,210,2,255],[255,255,255,255]]),m=g([[0,22,64,80],[0,57,102,127],[49,61,102,255],[225,43,2,255],[255,210,2,255],[255,255,255,255]]),n=g([[0,0,0,127],[64,64,64,255],[255,255,255,255]]),o=g([[128,0,38,127],[189,0,38,255],[227,26,28,255],[252,78,42,255],[253,141,60,255]]),p=g([[0,64,38,80],[0,90,50,127],[35,132,67,255],[65,171,93,255],[120,198,121,255]]),q=function(a){return function(b,c){var d=a[Math.floor(b*(f-1))];return c[0]=d[0],c[1]=d[1],c[2]=d[2],c[3]=d[3],c}},e.cool=q(h),e.hot=q(i),e.fire=q(j),e.verdant=q(k),e.spectral=q(l),e.temperature=q(m),e.grey=q(n),r=q(o),s=q(p),e.valence=function(a,b){return a>.5?r(2*(a-.5),b):s(2*a,b)},e.valence=function(a,b){return a>.5?s(2*(a-.5),b):.5>a?r(2*(.5-a),b):(b[0]=0,b[1]=0,b[2]=0,b[3]=0,b)},a("ColorRamp",e)}}}),System.register("components/sparse-binary-layer.js",["leaflet","../util/color-ramp"],function(a){"use strict";var b,c,d,e;return{setters:[function(a){b=a["default"]},function(a){c=a.ColorRamp}],execute:function(){d=function(a,b){var c=new XMLHttpRequest;c.open("GET",a,!0),c.responseType="json",c.onload=function(a){b(200==this.status?this.response:null)},c.send()},e=b.TileLayer.Canvas.extend({options:{async:!0,tms:!0,noWrap:!0,transform:"log10",ramp:"temperature"},initialize:function(a,c){this._url=a,b.setOptions(this,c),this._selected=null},onAdd:function(a){var c=this;b.TileLayer.Canvas.prototype.onAdd.call(this,a),a.on("click",this._onClick,this),a.on("mousemove",this._onHover,this),a.on("mouseout",function(){c._hovered=null;for(var a in c._tiles)c._redrawTile(c._tiles[a])})},_hitTest:function(a){var c=a.originalEvent.target,d=this._map.containerPointToLayerPoint(a.containerPoint),e=b.DomUtil.getPosition(c);if(e){var f=d.x-e.x,g=d.y-e.y;if(c&&c._tileData){var h=c._tileData,i=h.filter(function(a){return Math.abs(a.x-f)<5&&Math.abs(a.y-g)<5});if(i.length>0)return i[0].value}}},_onClick:function(a){var b=this._hitTest(a);this.fire("select",{id:b})},_onHover:function(a){var b=this._hitTest(a);if(b!==this._hovered){this._hovered=b;for(var c in this._tiles)this._redrawTile(this._tiles[c])}},setSelected:function(a){this._selected=a;for(var b in this._tiles)this._redrawTile(this._tiles[b])},_loadTile:function(a,b){a._layer=this,a._tilePoint=b,this._adjustTilePoint(b),this._redrawTile(a),this.options.async||this.tileDrawn(a)},_render:function(a,b,d){var e=this.options,f=this._selected,g=this._hovered,h=c[e.ramp],i=[];a._tileDataPt=b,a._tileData=d;var j=a.getContext("2d");if(j.clearRect(0,0,a.width,a.height),!d||!f&&!g)return void this.tileDrawn(a);for(var i,k=j.getImageData(0,0,a.width,a.height),l=k.data,m=h(.6,[]),n=h(.25,[]),o=0;o<d.length;o++){var p=d[o];if(i=p.value===f?m:p.value===g?n:null){var q=4*(p.y*a.width+p.x);l[q]=i[0],l[q+1]=i[1],l[q+2]=i[2],l[q+3]=i[3]}}j.putImageData(k,0,0),this.tileDrawn(a)},drawTile:function(a,b,c){var e=this,f=this.getTileUrl(b);a._tileDataPt===b?this._render(a,b,a._tileData):d(f,function(c){e._render(a,b,c)})}}),a("SparseBinaryTileLayer",e)}}}),System.register("components/map.js",["leaflet","../util/leaflet-1d","./binary-layer","./sparse-binary-layer","aurelia-framework","aurelia-pal","jquery","moment"],function(a){"use strict";function b(a){return a.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g,",")}var c,d,e,f,g,h,i,j,k,l,m,n;return{setters:[function(a){c=a["default"]},function(a){d=a.fixDrag,e=a.fixZoom},function(a){f=a.BinaryTileLayer},function(a){g=a.SparseBinaryTileLayer},function(a){h=a.bindable,i=a.inject,j=a.ObserverLocator},function(a){k=a.DOM},function(a){l=a["default"]},function(a){m=a["default"]}],execute:function(){n=function(){function a(a){babelHelpers.classCallCheck(this,o),babelHelpers.defineDecoratedPropertyDescriptor(this,"densityLayer",n),babelHelpers.defineDecoratedPropertyDescriptor(this,"hitLayer",n),babelHelpers.defineDecoratedPropertyDescriptor(this,"selectedId",n),babelHelpers.defineDecoratedPropertyDescriptor(this,"moveToTimestamp",n),babelHelpers.defineDecoratedPropertyDescriptor(this,"bounds",n),babelHelpers.defineDecoratedPropertyDescriptor(this,"tweetPromise",n),this.tweets={},this.observerLocator=a}var n={},n={};babelHelpers.createDecoratedClass(a,[{key:"densityLayer",decorators:[h],initializer:function(){return null},enumerable:!0},{key:"hitLayer",decorators:[h],initializer:function(){return null},enumerable:!0},{key:"selectedId",decorators:[h],initializer:function(){return null},enumerable:!0},{key:"moveToTimestamp",decorators:[h],initializer:function(){return null},enumerable:!0},{key:"bounds",decorators:[h],initializer:function(){return null},enumerable:!0},{key:"tweetPromise",decorators:[h],initializer:function(){return null},enumerable:!0}],null,n),babelHelpers.createDecoratedClass(a,[{key:"attached",value:function(){var a=this;this.tweetPromise.then(function(b){var c=!0,d=!1,e=void 0;try{for(var f,g=b[Symbol.iterator]();!(c=(f=g.next()).done);c=!0){var h=f.value;a.tweets[h.orig_id_str]=h}}catch(i){d=!0,e=i}finally{try{!c&&g["return"]&&g["return"]()}finally{if(d)throw e}}}),this.map=new c.Map(this.mapContainer,{zoomControl:!1,center:[-85.05113,180],zoom:3,minZoom:2,maxZoom:4,boxZoom:!1,touchZoom:!1,zoomAnimation:!1,maxBounds:[[-85.05113,-180],[85.05113,180]]}),new c.Control.Zoom({position:"topright"}).addTo(this.map),d(this.map),e(this.map),this.map.attributionControl.setPrefix('<a href="http://uncharted.software/salt" target="_blank">Uncharted Salt</a> | <a href="http://leafletjs.com" target="_blank">Leaflet</a>'),this.densityLayer&&this.densityLayer.metadata().then(function(b){a._addBinaryLayer(a.densityLayer.tileUrl(),b)}),this.hitLayer&&this._addHitLayer(this.hitLayer.tileUrl()),this.observerLocator.getObserver(this,"selectedId").subscribe(function(b){return a.onSelectionChange(b)}),this.onSelectionChange(this.selectedId),this.observerLocator.getObserver(this,"moveToTimestamp").subscribe(function(b){if(!isNaN(b)){var c=(b-a.bounds.x.min)/(a.bounds.x.max-a.bounds.x.min),d=[-76.98014914976216,360*c-180];a.map.panTo(d)}});var f=l(this.xPosition),g=l(this.yPosition),h=f.find(".label"),i=g.find(".label"),j=!1;this.map.on("mousemove",function(c){if(c.latlng.lat<85&&c.latlng.lat>-85&&c.latlng.lng>-180&&c.latlng.lng<180){j||(j=!0,f.show(),g.show()),f.css({left:c.containerPoint.x,height:a.map.getSize().y-c.containerPoint.y});var d=(c.latlng.lng+180)/360*(a.bounds.x.max-a.bounds.x.min)+a.bounds.x.min;h.html(m(d).format("MMM D, h:mmA")),g.css({top:c.containerPoint.y,width:c.containerPoint.x});var e=(a.map.getSize().y-c.containerPoint.y)/1536*175e3;i.html(b(e)+" retweets")}else j&&(f.hide(),g.hide(),j=!1)}),this.map.on("mouseout",function(){f.hide(),g.hide(),j=!1}),this.map.on("resize",function(b){a.map.setView([0,0],a.map.getZoom())})}},{key:"onSelectionChange",value:function(a){var b=this;this.densityLayer.metadata().then(function(c){if(b.hitLayer.setSelected(a),a){b.binaryLayer.setOpacity(.5);var d=b.tweets[a];if(d){var e=b.map.getSize().y/1536,f=(b.bounds.y.max-b.bounds.y.min)*e;if(d.orig_retweet_count>f){var g=k.createCustomEvent("offscreen",{detail:{value:d},bubbles:!0});b.mapContainer.dispatchEvent(g)}}}else b.binaryLayer.setOpacity(1)})}},{key:"_addBinaryLayer",value:function(a,b){this.binaryLayer=new f(a,{opacity:1,tms:!0,noWrap:!0,transform:"log10",ramp:"hot",zIndex:1,dataBounds:b}),this.retweetBounds=b,this.binaryLayer.addTo(this.map)}},{key:"_addHitLayer",value:function(a){var b=this;this.hitLayer=new g(a,{opacity:1,tms:!0,noWrap:!0,transform:"log10",ramp:"cool",zIndex:10}),this.hitLayer.addTo(this.map),this.hitLayer.on("select",function(a){var c=k.createCustomEvent("select",{detail:{value:a.id},bubbles:!0});b.mapContainer.dispatchEvent(c)})}}],null,n);var o=a;return a=i(j)(a)||a}(),a("Map",n)}}}),function(){var a=System.get("@@amd-helpers").createDefine();define("components/tweet-list.html!github:systemjs/plugin-text@0.0.3.js",[],function(){return'<template> <require from="../converters/date-format"></require> <ol class="tweet-list"> <li repeat.for="tweet of tweets" class="${tweet.orig_id_str === selectedId ? \'active\' : \'\'}" click.trigger="tweetSelected(tweet)"> <strong>${tweet.orig_user_name}</strong>&nbsp;<span class="text-muted tweet-meta">${tweet.orig_retweet_count} retweets</span><br> <span class="tweet-text text-muted">"${tweet.orig_text}"</span> </li> </ol> </template>'}),a()}(),System.register("components/tweet-list.js",["aurelia-framework","aurelia-pal"],function(a){"use strict";var b,c,d,e;return{setters:[function(a){b=a.bindable,c=a.inject},function(a){d=a.DOM}],execute:function(){e=function(){function a(a){babelHelpers.classCallCheck(this,f),babelHelpers.defineDecoratedPropertyDescriptor(this,"tweetPromise",e),this.tweets=[],this.element=a}var e={},e={};babelHelpers.createDecoratedClass(a,[{key:"tweetPromise",decorators:[b],initializer:function(){return null},enumerable:!0}],null,e),babelHelpers.createDecoratedClass(a,[{key:"attached",value:function(){var a=this;this.tweetPromise.then(function(b){return a.tweets=b})}},{key:"tweetSelected",value:function(a){var b=d.createCustomEvent("select",{detail:{value:a.orig_id_str},bubbles:!0});this.element.dispatchEvent(b);var c=new Date(a.orig_created_at).getTime();b=d.createCustomEvent("moveto",{detail:{value:c},bubbles:!0}),this.element.dispatchEvent(b)}}],null,e);var f=a;return a=c(Element)(a)||a}(),a("TweetList",e)}}}),function(){var a=System.get("@@amd-helpers").createDefine();define("components/twitter-card.html!github:systemjs/plugin-text@0.0.3.js",[],function(){return'<template> <h3 show.bind="tweetId === null"></h3> <div class="missing-tweet" show.bind="hasError"> <h2>This tweet has been deleted!</h2> </div> <div class="offscreen" show.bind="offscreen"> <span>Note: some retweet values are <em>offscreen</em></span> </div> <div ref="tweetContainer"></div> </template>'}),a()}(),System.register("services/twitter.js",[],function(a){"use strict";var b;return{setters:[],execute:function(){window.twttr=function(a,b,c){var d,e=a.getElementsByTagName(b)[0],f=window.twttr||{};return a.getElementById(c)?f:(d=a.createElement(b),d.id=c,d.src="https://platform.twitter.com/widgets.js",e.parentNode.insertBefore(d,e),f._e=[],f.ready=function(a){f._e.push(a)},f)}(document,"script","twitter-wjs"),b=function(){function a(){babelHelpers.classCallCheck(this,a)}return babelHelpers.createClass(a,[{key:"api",get:function(){return new Promise(function(a,b){twttr.ready(function(b){return a(b)})})}}]),a}(),a("Twitter",b)}}}),System.register("components/twitter-card.js",["aurelia-framework","aurelia-pal","../services/twitter"],function(a){"use strict";var b,c,d,e,f,g;return{setters:[function(a){b=a.bindable,c=a.inject,d=a.ObserverLocator},function(a){e=a.DOM},function(a){f=a.Twitter}],execute:function(){g=function(){function a(a,b){babelHelpers.classCallCheck(this,g),babelHelpers.defineDecoratedPropertyDescriptor(this,"tweetId",e),babelHelpers.defineDecoratedPropertyDescriptor(this,"offscreen",e),this.hasError=null,this.twitter=a,this.observerLocator=b}var e={},e={};babelHelpers.createDecoratedClass(a,[{key:"tweetId",decorators:[b],initializer:function(){return null},enumerable:!0},{key:"offscreen",decorators:[b],initializer:function(){return null},enumerable:!0}],null,e),babelHelpers.createDecoratedClass(a,[{key:"attached",value:function(){var a=this;this.stopObserving=this.observerLocator.getObserver(this,"tweetId").subscribe(function(b){return a.createWidget()}),this.createWidget()}},{key:"detached",value:function(){this.stopObserving()}},{key:"createWidget",value:function(){for(var a=this;this.tweetContainer.hasChildNodes();)this.tweetContainer.removeChild(this.tweetContainer.lastChild);this.tweetId&&this.twitter.api.then(function(b){b.widgets.createTweet(a.tweetId,a.tweetContainer,{cards:window.innerWidth>992?void 0:"hidden",conversation:"none",theme:"light",linkColor:"#af520e"}).then(function(b){a.hasError=!b})})}}],null,e);var g=a;return a=c(f,d)(a)||a}(),a("TwitterCard",g)}}}),System.register("converters/date-format.js",["moment"],function(a){"use strict";var b,c;return{setters:[function(a){b=a["default"]}],execute:function(){c=function(){function a(){babelHelpers.classCallCheck(this,a)}return babelHelpers.createClass(a,[{key:"toView",value:function(a){return b(new Date(a)).format("MMM D, YYYY")}}]),a}(),a("DateFormatValueConverter",c)}}});