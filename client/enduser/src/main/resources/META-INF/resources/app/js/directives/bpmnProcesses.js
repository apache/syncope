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
angular.module('self').directive('bpmnProcesses', [function () {
    return {
      restrict: 'E',
      templateUrl: 'views/bpmnProcesses.html',
      scope: {
      },
      controller: function ($scope) {
        $scope.bpmnProcesses = $scope.$parent.$parent.resolve.bpmnProcesses;
        $scope.selectedProcesses = $scope.$parent.$parent.resolve.selectedProcesses;
        $scope.toggleSelection =
                function (bpmnProcessKey) {
                  var index = $scope.selectedProcesses.indexOf(bpmnProcessKey);
                  if (index > -1) {
                    $scope.selectedProcesses.splice(index, 1);
                  } else {
                    $scope.selectedProcesses.push(bpmnProcessKey);
                  }
                };
      }};
  }]);

