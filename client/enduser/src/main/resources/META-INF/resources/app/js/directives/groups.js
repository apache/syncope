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
        .directive('groups', ['GroupService', function (GroupService) {
            return {
              restrict: 'E',
              templateUrl: 'views/groups.html',
              scope: {
                dynamicForm: "=form",
                user: "="
              },
              controller: function ($scope, $filter, $timeout) {
                var groupsSearchTimer;

                $scope.init = function () {
                  if (!$scope.user.memberships) {
                    $scope.user.memberships = new Array();
                  }
                };

                $scope.addGroup = function (item, model) {
                  var membership = item;
                  $scope.user.memberships.push({"groupKey": membership.groupKey, "groupName": membership.groupName});
                  $scope.$emit("groupAdded", membership.groupName);
                };

                $scope.removeGroup = function (item, model) {
                  var groupIndex = $scope.getIndex(item);
                  var membership = $scope.user.memberships[groupIndex];
                  var groupName = membership.groupName;
                  $scope.user.memberships.splice(groupIndex, 1);
                  $scope.$emit("groupRemoved", groupName);
                };

                $scope.onGroupsSearch = function (totGroups) {
                  if (groupsSearchTimer) {
                    $timeout.cancel(groupsSearchTimer);
                  }
                  if (totGroups > 30) {
                    var that = this;
                    groupsSearchTimer = $timeout(function () {
                      $scope.$emit("groupSearched", that.$select);
                    }, 800);
                  }
                };

                $scope.getIndex = function (selectedGroup) {
                  var groupIndex = $scope.user.memberships.map(function (membership) {
                    return membership.groupName;
                  }).indexOf(selectedGroup.groupName);
                  return groupIndex;
                };
              }
            };
          }]);
