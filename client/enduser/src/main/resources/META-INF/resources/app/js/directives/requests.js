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
        .directive('requests', ['UserRequestsService', 'BpmnProcessService', '$rootScope',
          function (UserRequestsService, BpmnProcessService, $rootScope) {
            return {
              restrict: 'E',
              templateUrl: 'views/requests.html',
              scope: {
                user: "="
              },
              controller: function ($scope) {
                // Initialization
                $scope.query = {
                  user: $scope.user.username,
                  page: 1,
                  size: 10
                };

                var calculatePages = function () {
                  $scope.totalPages = Math.ceil($scope.requests.totalCount / $scope.query.size);
                  $scope.pages = _.range(1, $scope.totalPages + 1);
                };

                // <Pagination>
                $scope.reloadPage = function (page, size, successMsg) {
                  // update query pagination parameters
                  $scope.query.page = page;
                  $scope.query.size = size;

                  $scope.getUserRequests($scope.query, function (requests) {
                    $scope.requests = requests;
                    if (successMsg) {
                      $scope.$parent.showSuccess(successMsg, $scope.$parent.notification);
                    }
                    // recalculate pages
                    calculatePages();
                  });
                };
                // </Pagination>

                $scope.getUserRequests = function (query, callback) {
                  UserRequestsService.getUserRequests(query).then(function (response) {
                    callback(response);
                  }, function (response) {
                    var errorMessage;
                    // parse error response 
                    console.log("ERROR ", response);
                    if (response !== undefined) {
                      errorMessage = response.split("ErrorMessage{{")[1];
                      errorMessage = errorMessage.split("}}")[0];
                    }
                    console.error("Error retrieving User Requests: ", errorMessage);
                    $scope.$parent.showError("Error: " + (errorMessage || response), $scope.$parent.notification);
                  });
                };

                var init = function () {
                  $scope.requests = [];
                  $scope.availableSizes = [{id: 1, value: 10}, {id: 2, value: 25}, {id: 3, value: 50}];
                  $scope.selectedSize = $scope.availableSizes[0];
                  // date formatting
                  $scope.formatDate = $rootScope.formatDate;
                  // init requests
                  $scope.getUserRequests($scope.query, function (requests) {
                    $scope.requests = requests;
                    calculatePages();
                  });
                };

                var initBpmnProcesses = function () {
                  $scope.selectedBpmnProcess = "";
                  $scope.bpmnProcesses = [];
                  BpmnProcessService.getBpmnProcesses().then(function (response) {
                    $scope.bpmnProcesses = response;
                  }, function (response) {
                    // parse error response and log
                    if (response !== undefined) {
                      var errorMessages = response.toString().split("ErrorMessage{{");
                      if (errorMessages.length > 1) {
                        console.error("Error retrieving BPMN Processes: ", response.toString()
                                .split("ErrorMessage{{")[1].split("}}")[0]);
                      } else {
                        console.error("Error retrieving BPMN Processes: ", errorMessages);
                      }
                    }
                  });
                };

                init();
                initBpmnProcesses();

                $scope.cancel = function (requestWrapper, reason) {
                  console.log("Cancel request ", requestWrapper.request.executionId, reason);
                  UserRequestsService.cancel(requestWrapper.request.executionId, reason).then(function (response) {
                    var index = $scope.requests.result.indexOf(requestWrapper);
                    if (index > -1) {
                      $scope.requests.result.splice(index, 1);
                      $scope.requests.totalCount--;
                      $scope.reloadPage($scope.query.page, $scope.query.size,
                              "Process " + requestWrapper.request.executionId + " successfully canceled");
                    }
                  }, function (response) {
                    var errorMessage;
                    // parse error response 
                    if (response !== undefined) {
                      errorMessage = response.split("ErrorMessage{{")[1];
                      errorMessage = errorMessage.split("}}")[0];
                    }
                    console.error("Error canceling User Request: ", requestWrapper.executionId, errorMessage);
                  });
                };

                $scope.startRequest = function () {
                  var currentProc = $scope.selectedBpmnProcess;
                  UserRequestsService.start(currentProc).then(function (response) {
                    console.log("Process " + currentProc + " successfully started");
                    $scope.reloadPage($scope.query.page, $scope.query.size,
                            "Process " + currentProc + " successfully started");
                  }, function (response) {
                    var errorMessage;
                    // parse error response 
                    if (response !== undefined) {
                      errorMessage = response.split("ErrorMessage{{")[1];
                      errorMessage = errorMessage.split("}}")[0];
                    }
                    $scope.$parent.showError("Error: " + (errorMessage || response), $scope.$parent.notification);
                    console.error("Error starting User Request: ", errorMessage);
                  });
                };

                $scope.submitForm = function (form) {
                  UserRequestsService.submitForm(form).then(function (response) {
                    console.debug("Form successfully submitted");
                    $scope.$parent.showSuccess("Form successfully submitted", $scope.$parent.notification);
                    $scope.reloadPage($scope.query.page, $scope.query.size, "Form successfully submitted");
                  }, function (response) {
                    var errorMessage;
                    // parse error response 
                    if (response !== undefined) {
                      errorMessage = response.split("ErrorMessage{{")[1];
                      errorMessage = errorMessage.split("}}")[0];
                    }
                    console.error("Error retrieving User Request Forms: ", errorMessage);
                    $scope.$parent.showError("Error: " + (errorMessage || response), $scope.$parent.notification);
                  });

                };
                
              }
            };
          }]);