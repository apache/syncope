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
        .factory('UserRequestsService', ['$q', '$http', function ($q, $http) {

            var userRequestsService = {};

            userRequestsService.getUserRequests = function (query) {
              return  $http.get("../api/flowable/userRequests?user=" + query.user
                      + (query.page ? "&page=" + query.page + (query.size ? "&size=" + query.size : "") : "")
                      + (query.orderBy ? "&orderBy=" + query.orderby : "") + "&withForm=true")
                      .then(function (response) {
                        return response.data;
                      }, function (response) {
                        return $q.reject(response.data || response.statusText);
                      });
            };

            userRequestsService.submitForm = function (form) {
              return  $http.post("../api/flowable/userRequests/forms", form)
                      .then(function (response) {
                        return response.data;
                      }, function (response) {
                        return $q.reject(response.data || response.statusText);
                      });
            };

            userRequestsService.cancel = function (executionId, reason) {
              return  $http.delete("../api/flowable/userRequests?executionId=" + executionId
                      + (reason ? "&reason=" + reason : ""))
                      .then(function (response) {
                        return response.data;
                      }, function (response) {
                        return $q.reject(response.data || response.statusText);
                      });
            };

            userRequestsService.start = function (bpmnProcess) {
              return  $http.post("../api/flowable/userRequests/start/" + bpmnProcess)
                      .then(function (response) {
                        return response.data;
                      }, function (response) {
                        return $q.reject(response.data || response.statusText);
                      });
            };

            return userRequestsService;
          }]);
