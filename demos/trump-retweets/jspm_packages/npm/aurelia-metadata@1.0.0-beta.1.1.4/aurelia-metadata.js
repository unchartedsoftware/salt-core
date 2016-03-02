/* */ 
define(['exports', 'core-js', 'aurelia-pal'], function (exports, _coreJs, _aureliaPal) {
  'use strict';

  exports.__esModule = true;

  var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

  exports.decorators = decorators;
  exports.deprecated = deprecated;
  exports.mixin = mixin;
  exports.protocol = protocol;

  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

  var theGlobal = _aureliaPal.PLATFORM.global;
  var emptyMetadata = Object.freeze({});
  var metadataContainerKey = '__metadata__';

  if (typeof theGlobal.Reflect === 'undefined') {
    theGlobal.Reflect = {};
  }

  if (typeof theGlobal.Reflect.getOwnMetadata === 'undefined') {
    Reflect.getOwnMetadata = function (metadataKey, target, targetKey) {
      return ((target[metadataContainerKey] || emptyMetadata)[targetKey] || emptyMetadata)[metadataKey];
    };
  }

  if (typeof theGlobal.Reflect.defineMetadata === 'undefined') {
    Reflect.defineMetadata = function (metadataKey, metadataValue, target, targetKey) {
      var metadataContainer = target.hasOwnProperty(metadataContainerKey) ? target[metadataContainerKey] : target[metadataContainerKey] = {};
      var targetContainer = metadataContainer[targetKey] || (metadataContainer[targetKey] = {});
      targetContainer[metadataKey] = metadataValue;
    };
  }

  if (typeof theGlobal.Reflect.metadata === 'undefined') {
    Reflect.metadata = function (metadataKey, metadataValue) {
      return function (target, targetKey) {
        Reflect.defineMetadata(metadataKey, metadataValue, target, targetKey);
      };
    };
  }

  var metadata = {
    resource: 'aurelia:resource',
    paramTypes: 'design:paramtypes',
    properties: 'design:properties',
    get: function get(metadataKey, target, targetKey) {
      if (!target) {
        return undefined;
      }
      var result = metadata.getOwn(metadataKey, target, targetKey);
      return result === undefined ? metadata.get(metadataKey, Object.getPrototypeOf(target), targetKey) : result;
    },
    getOwn: function getOwn(metadataKey, target, targetKey) {
      if (!target) {
        return undefined;
      }
      return Reflect.getOwnMetadata(metadataKey, target, targetKey);
    },
    define: function define(metadataKey, metadataValue, target, targetKey) {
      Reflect.defineMetadata(metadataKey, metadataValue, target, targetKey);
    },
    getOrCreateOwn: function getOrCreateOwn(metadataKey, Type, target, targetKey) {
      var result = metadata.getOwn(metadataKey, target, targetKey);

      if (result === undefined) {
        result = new Type();
        Reflect.defineMetadata(metadataKey, result, target, targetKey);
      }

      return result;
    }
  };

  exports.metadata = metadata;
  var originStorage = new Map();
  var unknownOrigin = Object.freeze({ moduleId: undefined, moduleMember: undefined });

  var Origin = (function () {
    function Origin(moduleId, moduleMember) {
      _classCallCheck(this, Origin);

      this.moduleId = moduleId;
      this.moduleMember = moduleMember;
    }

    Origin.get = function get(fn) {
      var origin = originStorage.get(fn);

      if (origin === undefined) {
        _aureliaPal.PLATFORM.eachModule(function (key, value) {
          for (var _name in value) {
            var exp = value[_name];
            if (exp === fn) {
              originStorage.set(fn, origin = new Origin(key, _name));
              return true;
            }
          }

          if (value === fn) {
            originStorage.set(fn, origin = new Origin(key, 'default'));
            return true;
          }
        });
      }

      return origin || unknownOrigin;
    };

    Origin.set = function set(fn, origin) {
      originStorage.set(fn, origin);
    };

    return Origin;
  })();

  exports.Origin = Origin;

  function decorators() {
    for (var _len = arguments.length, rest = Array(_len), _key = 0; _key < _len; _key++) {
      rest[_key] = arguments[_key];
    }

    var applicator = function applicator(target, key, descriptor) {
      var i = rest.length;

      if (key) {
        descriptor = descriptor || {
          value: target[key],
          writable: true,
          configurable: true,
          enumerable: true
        };

        while (i--) {
          descriptor = rest[i](target, key, descriptor) || descriptor;
        }

        Object.defineProperty(target, key, descriptor);
      } else {
        while (i--) {
          target = rest[i](target) || target;
        }
      }

      return target;
    };

    applicator.on = applicator;
    return applicator;
  }

  function deprecated(optionsOrTarget, maybeKey, maybeDescriptor) {
    function decorator(target, key, descriptor) {
      var methodSignature = target.constructor.name + '#' + key;
      var options = maybeKey ? {} : optionsOrTarget || {};
      var message = 'DEPRECATION - ' + methodSignature;

      if (typeof descriptor.value !== 'function') {
        throw new SyntaxError('Only methods can be marked as deprecated.');
      }

      if (options.message) {
        message += ' - ' + options.message;
      }

      return _extends({}, descriptor, {
        value: function deprecationWrapper() {
          if (options.error) {
            throw new Error(message);
          } else {
            console.warn(message);
          }

          return descriptor.value.apply(this, arguments);
        }
      });
    }

    return maybeKey ? decorator(optionsOrTarget, maybeKey, maybeDescriptor) : decorator;
  }

  function mixin(behavior) {
    var instanceKeys = Object.keys(behavior);

    function _mixin(possible) {
      var decorator = function decorator(target) {
        var resolvedTarget = typeof target === 'function' ? target.prototype : target;

        for (var _iterator = instanceKeys, _isArray = Array.isArray(_iterator), _i = 0, _iterator = _isArray ? _iterator : _iterator[Symbol.iterator]();;) {
          var _ref;

          if (_isArray) {
            if (_i >= _iterator.length) break;
            _ref = _iterator[_i++];
          } else {
            _i = _iterator.next();
            if (_i.done) break;
            _ref = _i.value;
          }

          var property = _ref;

          Object.defineProperty(resolvedTarget, property, {
            value: behavior[property],
            writable: true
          });
        }
      };

      return possible ? decorator(possible) : decorator;
    }

    return _mixin;
  }

  function alwaysValid() {
    return true;
  }
  function noCompose() {}

  function ensureProtocolOptions(options) {
    if (options === undefined) {
      options = {};
    } else if (typeof options === 'function') {
      options = {
        validate: options
      };
    }

    if (!options.validate) {
      options.validate = alwaysValid;
    }

    if (!options.compose) {
      options.compose = noCompose;
    }

    return options;
  }

  function createProtocolValidator(validate) {
    return function (target) {
      var result = validate(target);
      return result === true;
    };
  }

  function createProtocolAsserter(name, validate) {
    return function (target) {
      var result = validate(target);
      if (result !== true) {
        throw new Error(result || name + ' was not correctly implemented.');
      }
    };
  }

  function protocol(name, options) {
    options = ensureProtocolOptions(options);

    var result = function result(target) {
      var resolvedTarget = typeof target === 'function' ? target.prototype : target;

      options.compose(resolvedTarget);
      result.assert(resolvedTarget);

      Object.defineProperty(resolvedTarget, 'protocol:' + name, {
        enumerable: false,
        configurable: false,
        writable: false,
        value: true
      });
    };

    result.validate = createProtocolValidator(options.validate);
    result.assert = createProtocolAsserter(name, options.validate);

    return result;
  }

  protocol.create = function (name, options) {
    options = ensureProtocolOptions(options);
    var hidden = 'protocol:' + name;
    var result = function result(target) {
      var decorator = protocol(name, options);
      return target ? decorator(target) : decorator;
    };

    result.decorates = function (obj) {
      return obj[hidden] === true;
    };
    result.validate = createProtocolValidator(options.validate);
    result.assert = createProtocolAsserter(name, options.validate);

    return result;
  };
});