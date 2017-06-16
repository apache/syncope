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
angular.module('SyncopeEnduserApp')
        .factory('ValidationExecutor', ['GenericUtil',
          function (GenericUtil) {

            var validationExecutor = {};

            validationExecutor.validate = function (form, scope) {
              if (scope.validationEnabled) {
                var currentForm = scope[form.name] || scope[form.$name];
                angular.forEach(currentForm.$error, function (field) {
                  for (var i in field) {
                    scope.$root.$broadcast(field[i].$name, {errors: field[i].$error});
                  }
                });
                currentForm.$setSubmitted();
                if ($.isEmptyObject(currentForm.$error)) {
                  return true;
                } else {
                  scope.showError("Data are invalid: please correct accordingly", scope.notification);
                  return false;
                }
              } else
                return true;
            };
            return validationExecutor;
          }]);