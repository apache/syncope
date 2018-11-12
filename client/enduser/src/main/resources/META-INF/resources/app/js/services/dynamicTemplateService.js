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

'use strict';

angular.module('SyncopeEnduserApp')
        .factory('DynamicTemplateService', ['$q', '$http', 'AssetsManager',
          function ($q, $http, AssetsManager) {

            var dynTemplateService = {};
            var dynTemplateUrl = '../api/dynamicTemplate';

            var error = function (response) {
              console.error("Something went wrong while retrieving dynamic template resource", response);
              return $q.reject(response.data || response.statusText);
            };

            var loadAssets = function (category, assets, types) {
              var allPromises = types.reduce((acc, type) => {
                if (assets[category][type]) {
                  var currentAssetsPromises =
                          assets[category][type].map((url, index) => AssetsManager.
                            inject("elem_" + index, url, type));
                  return acc.concat(currentAssetsPromises);
                }
              }, []);

              return $q.all(allPromises);
            };

            dynTemplateService.getContent = function () {
              return $http.
                      get(dynTemplateUrl).
                      then(function (response) {
                        return response.data;
                      }, error);
            };

            dynTemplateService.getGeneralAssetsContent = function (types) {
              return $http.
                      get(dynTemplateUrl).
                      then(function (response) {
                        return loadAssets("generalAssets", response.data, types);
                      }, error);
            };

            return dynTemplateService;

          }]);


