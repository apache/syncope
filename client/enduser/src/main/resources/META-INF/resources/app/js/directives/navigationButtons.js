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
        .directive('navigationButtons', ['$state','GenericUtil', 'PolicyValidator', function ($state, GenericUtil, PolicyValidator) {
            return {
              restrict: 'E',
              templateUrl: 'views/navigationButtons.html',
              scope: {
                next: "@",
                previous: "@",
              },
              controller: function ($scope) {

                $scope.validateAndNext = function (event, state) {
                  //getting the enclosing form in order to access to its name                
                  var currentForm = GenericUtil.getEnclosingForm(event.target);
                  if (currentForm != null) {
                    if (PolicyValidator.validate(currentForm, $scope.$parent)) {
                      $scope.nextTab(state);
                    }
                  }

                };

                $scope.nextTab = function (state) {
                  //change route through parent event
                  $state.go(state);
                };
              }

            }
            ;
          }]);
