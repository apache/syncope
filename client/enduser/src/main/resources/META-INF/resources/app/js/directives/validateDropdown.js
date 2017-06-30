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

//Live validation
angular.module('SyncopeEnduserApp').
        directive('validateDropdown', ['$timeout', function ($timeout) {

            return {
              restrict: 'A',
              require: 'ngModel',
              replace: true,

              link: function ($scope, $elem, attrs, ngModel) {

                $scope.dynamicform = $scope.$eval(attrs.dynamicForm);
                var dropdownSelectName = attrs.validateDropdown;

                var realmTemp;
                var isGroups = dropdownSelectName === "groups";
                if (isGroups) {
                  realmTemp = $scope.user.realm;
                }

                // enable / disable input according to available values
                $scope.$watch(attrs.dynamicForm + "." + dropdownSelectName, function (newValues) {
                  // disable input element when no data is available (groups, resources, etc...)
                  if (newValues && newValues.length > 0) {
                    $scope.$parent.inputDisabled = false;
                  } else {
                    $scope.$parent.inputDisabled = true;
                  }
                  // to solve some graphical rerendering issues
                  $timeout(function () {
                    $elem.trigger('click');
                  }, 100);

                  // remove selected groups when realm changes
                  if (isGroups && realmTemp !== $scope.user.realm) {
                    // remove group list only whether selected groups are not in realms

                    for (var item in $scope.dynamicform.selectedGroups) {
                      var filtered = newValues.filter(function (e) {
                        return $scope.dynamicform.selectedGroups[item] &&
                                e.groupName === $scope.dynamicform.selectedGroups[item].groupName;
                      });
                      if (filtered.length === 0) {
                        $scope.$parent.removeGroup($scope.dynamicform.selectedGroups[item], $scope.ngModel);
                        $scope.dynamicform.selectedGroups = _.without($scope.dynamicform.selectedGroups,
                                $scope.dynamicform.selectedGroups[item]);
                      }
                    }
                    realmTemp = $scope.user.realm;
                  }
                });

              }

            };
          }]);
