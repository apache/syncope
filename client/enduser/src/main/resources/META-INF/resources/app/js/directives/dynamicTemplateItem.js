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

angular.module('self')
        .directive('dynamicTemplateItem', ['$rootScope', '$compile', '$http', 'AssetsManager',
          function ($rootScope, $compile, $http, AssetsManager) {

            var checkGeneralAssets = function () {
              $rootScope.getDynamicTemplateOtherInfo("generalAssets", "css", function (assets) {
                if (assets && assets.length) {
                  for (var i = 0; i < assets.length; i++) {
                    if (!AssetsManager.checkAlreadyLoaded(assets[i], "css")) {
                      AssetsManager.inject("asset_general_css_" + i, assets[i], "css");
                    }
                  }
                }
              });
            };

            var linker = function ($scope, $element, $attrs) {
              // compile template
              $rootScope.getDynamicTemplateInfo($attrs.type, "templateUrl", function (templateUrl) {
                if (templateUrl) {
                  $http.get(templateUrl).then(function (response) {
                    $element.html(response.data).show();
                    $compile($element.contents())($scope);
                  }, function (e) {
                    console.error(e);
                  });
                }
              });

              // inject template assets
              $rootScope.getDynamicTemplateInfo($attrs.type, "css", function (assets) {
                if (assets && assets.length) {
                  for (var i = 0; i < assets.length; i++) {
                    AssetsManager.inject("asset_css_" + i, assets[i], "css");
                  }
                }
              });

              // remove useless assets for little optimization
              if ($attrs.type !== "login") {
                $rootScope.getDynamicTemplateInfo("login", "css", function (assets) {
                  for (var i = 0; i < assets.length; i++) {
                    AssetsManager.remove(assets[i], "css");
                  }
                });
              }

              // check general assets are always loaded (in case page refreshing in wizard)
              checkGeneralAssets();
            };

            return {
              restrict: "E",
              link: linker,
              replace: true
            };

          }]);
