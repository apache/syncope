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
        .directive('dynamicDerivedAttribute', function () {
          return {
            restrict: 'E',
            templateUrl: 'views/dynamicDerivedAttribute.html',
            scope: {
              schema: "=",
              index: "=",
              user: "="
            },
            controller: function ($scope) {
              $scope.$watch(function () {
                return $scope.user.derAttrs[$scope.schema.key].values[$scope.index];
              }, function (newValue, oldValue) {
                $scope.user.derAttrs[$scope.schema.key].values = $scope.user.derAttrs[$scope.schema.key].values
                        .filter(function (n) {
                          return (n !== undefined && n !== "");
                        });
              });
            },
            //replace: true
          };
        });
