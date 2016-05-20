/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*!
 * angular-translate - v2.11.0 - 2016-03-20
 * 
 * Copyright (c) 2016 The angular-translate team, Pascal Precht; Licensed MIT
 */
(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module unless amdModuleId is set
    define([], function () {
      return (factory());
    });
  } else if (typeof exports === 'object') {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory();
  } else {
    factory();
  }
}(this, function () {

$translateStaticFilesLoader.$inject = ['$q', '$http'];
angular.module('pascalprecht.translate')
/**
 * @ngdoc object
 * @name pascalprecht.translate.$translateStaticFilesLoader
 * @requires $q
 * @requires $http
 *
 * @description
 * Creates a loading function for a typical static file url pattern:
 * "lang-en_US.json", "lang-de_DE.json", etc. Using this builder,
 * the response of these urls must be an object of key-value pairs.
 *
 * @param {object} options Options object, which gets prefix, suffix and key.
 */
.factory('$translateStaticFilesLoader', $translateStaticFilesLoader);

function $translateStaticFilesLoader($q, $http) {

  'use strict';

  return function (options) {

    if (!options || (!angular.isArray(options.files) && (!angular.isString(options.prefix) || !angular.isString(options.suffix)))) {
      throw new Error('Couldn\'t load static files, no files and prefix or suffix specified!');
    }

    if (!options.files) {
      options.files = [{
        prefix: options.prefix,
        suffix: options.suffix
      }];
    }

    var load = function (file) {
      if (!file || (!angular.isString(file.prefix) || !angular.isString(file.suffix))) {
        throw new Error('Couldn\'t load static file, no prefix or suffix specified!');
      }

      return $http(angular.extend({
        url: [
          file.prefix,
          options.key,
          file.suffix
        ].join(''),
        method: 'GET',
        params: ''
      }, options.$http))
        .then(function(result) {
          return result.data;
        }, function () {
          return $q.reject(options.key);
        });
    };

    var promises = [],
        length = options.files.length;

    for (var i = 0; i < length; i++) {
      promises.push(load({
        prefix: options.files[i].prefix,
        key: options.key,
        suffix: options.files[i].suffix
      }));
    }

    return $q.all(promises)
      .then(function (data) {
        var length = data.length,
            mergedData = {};

        for (var i = 0; i < length; i++) {
          for (var key in data[i]) {
            mergedData[key] = data[i][key];
          }
        }

        return mergedData;
      });
  };
}

$translateStaticFilesLoader.displayName = '$translateStaticFilesLoader';
return 'pascalprecht.translate';

}));
