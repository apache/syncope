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
        .directive('dynamicVirtualAttribute', function () {
          return {
            restrict: 'E',
            templateUrl: 'views/dynamicVirtualAttribute.html',
            scope: {
              schema: "=",
              index: "=",
              user: "="
            },
            controller: function ($scope, $rootScope) {
              var customValues = $rootScope.customFormAttributes
                      && $rootScope.customFormAttributes["VIRTUAL"]
                      && $rootScope.customFormAttributes["VIRTUAL"]["attributes"]
                      && $rootScope.customFormAttributes["VIRTUAL"]["attributes"][$scope.schema.key]
                      && $rootScope.customFormAttributes["VIRTUAL"]["attributes"][$scope.schema.key].defaultValues
                      ? $rootScope.customFormAttributes["VIRTUAL"]["attributes"][$scope.schema.key].defaultValues
                      : [];

              $scope.$watch(function () {
                return $scope.user.virAttrs[$scope.schema.key].values[$scope.index];
              }, function (newValue, oldValue) {
                if ($scope.user.virAttrs[$scope.schema.key].values
                        && $scope.user.virAttrs[$scope.schema.key].values.length > 0) {
                  $scope.user.virAttrs[$scope.schema.key].values = $scope.user.virAttrs[$scope.schema.key].values
                          .filter(function (n) {
                            return (n !== undefined && n !== "");
                          });
                } else {
                  $scope.user.virAttrs[$scope.schema.key].values = customValues
                          .filter(function (n) {
                            return (n !== undefined && n !== "");
                          });
                }
              });

              $scope.customReadonly = function (schemaKey) {
                return  $rootScope.customFormAttributes
                        && $rootScope.customFormAttributes["VIRTUAL"]
                        && $rootScope.customFormAttributes["VIRTUAL"]["attributes"]
                        && $rootScope.customFormAttributes["VIRTUAL"]["attributes"][schemaKey]
                        && $rootScope.customFormAttributes["VIRTUAL"]["attributes"][schemaKey].readonly;
              };
            }
            //replace: true
          };

        });
