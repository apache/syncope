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
        .directive('autoCompleteTextField', function () {
          return {
            restrict: 'E',
            templateUrl: 'views/autoCompleteTextField.html',
            scope: {
              values: "=",
              selected: "="
            },
            controller: function ($scope) {
              $scope.complete = function (string) {
                var output = [];
                angular.forEach($scope.values, function (selected) {
                  if (selected.key.toLowerCase().indexOf(string.toLowerCase()) >= 0) {
                    output.push(selected.key);
                  }
                });
                $scope.filteredValues = output;
              };

              $scope.fillTextbox = function (selected) {
                $scope.selected = selected;
                $scope.filteredValues = null;
              };
            }
          };
        });
