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
        .directive('auxiliary', function () {
          return {
            restrict: 'E',
            templateUrl: 'views/auxClasses.html',
            scope: {
              dynamicForm: "=form",
              user: "="
            },
            controller: function ($scope, $filter) {

              $scope.init = function () {
                if (!$scope.user.auxClasses) {
                  $scope.user.auxClasses = new Array();                  
                }
              };

              $scope.addAuxClass = function (item, model) {
                var auxClass = item;
                $scope.user.auxClasses.push(auxClass);
                $scope.$emit("auxClassAdded", auxClass);
              };

              $scope.removeAuxClass = function (item, model) {
                var auxClassIndex = $scope.getIndex(item);
                $scope.user.auxClasses.splice(auxClassIndex, 1);
                $scope.$emit("auxClassRemoved", item);
              };

              $scope.getIndex = function (selectedAuxClass) {
                var auxClassIndex = $scope.user.auxClasses.map(function (auxClassName) {
                  return auxClassName;
                }).indexOf(selectedAuxClass);
                return auxClassIndex;
              };

            }
          };
        });
