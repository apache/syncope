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
        .directive('requests', ['UserRequestsService', 'BpmnProcessService', "$uibModal", "$document", '$filter',
          '$rootScope',
          function (UserRequestsService, BpmnProcessService, $uibModal, $document, $filter, $rootScope) {
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
                  // recalculate pages
                  calculatePages();
                  if (page < 1 || page > $scope.totalPages) {
                    return;
                  }
                  // get current page of items

                  $scope.getUserRequests($scope.query, function (requests) {
                    $scope.requests = requests;
                    if (successMsg) {
                      $scope.$parent.showSuccess(successMsg, $scope.$parent.notification);
                    }
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
                  $scope.getUserRequests($scope.query, function (requests) {
                    $scope.requests = requests;
                    calculatePages();
                    $scope.availableSizes = [{id: 1, value: 10}, {id: 2, value: 25}, {id: 3, value: 50}];
                    $scope.selectedSize = $scope.availableSizes[0];
                    // date formatting
                    $scope.formatDate = $rootScope.formatDate;
                  });

                };

                var initBpmnProcesses = function () {
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

                $scope.cancel = function (request, reason) {
                  console.log("Cancel request ", request.executionId, reason);
                  UserRequestsService.cancel(request.executionId, reason).then(function (response) {
                    var index = $scope.requests.result.indexOf(request);
                    if (index > -1) {
                      $scope.requests.result.splice(index, 1);
                      $scope.requests.totalCount--;
                      $scope.reloadPage($scope.query.page, $scope.query.size, 
                      "Process " + request.executionId + " successfully canceled");
                    }
                  }, function (response) {
                    var errorMessage;
                    // parse error response 
                    if (response !== undefined) {
                      errorMessage = response.split("ErrorMessage{{")[1];
                      errorMessage = errorMessage.split("}}")[0];
                    }
                    console.error("Error canceling User Request: ", request.executionId, errorMessage);
                  });
                };

                $scope.openComponentModal = function (size, parentSelector) {
                  $scope.selectedProcesses = [];
                  var parentElem = parentSelector ?
                          angular.element($document[0].querySelector(parentSelector)) : undefined;
                  var modalInstance = $uibModal.open({
                    animation: true,
                    ariaLabelledBy: 'modal-title',
                    ariaDescribedBy: 'modal-body',
                    component: 'modalWindow',
                    appendTo: parentElem,
                    size: size,
                    windowClass: 'in',
                    backdropClass: 'in',
                    resolve: {
                      bpmnProcesses: function () {
                        return $scope.bpmnProcesses;
                      },
                      selectedProcesses: function () {
                        return $scope.selectedProcesses;
                      },
                      modalHtml: function () {
                        return '<bpmn-processes></bpmn-processes>';
                      },
                      title: function () {
                        return $filter('translate')(["SELECT_PROCESS"]).SELECT_PROCESS;
                      }
                    }
                  });

                  modalInstance.result.then(function () {
                    for (var i = 0; i < $scope.selectedProcesses.length; i++) {
                      startRequest(i);
                    }
                  }, function () {
                  });
                };

                var startRequest = function (i) {
                  var currentProc = $scope.selectedProcesses[i];
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
              }
            };
          }]);