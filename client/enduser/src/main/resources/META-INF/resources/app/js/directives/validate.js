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
        directive('validate', function () {
          return {
            require: 'ngModel',
            link: function (scope, elem, attrs, ngModel) {
              scope.$watch(attrs.ngModel, function (value) {
                //trigger the live validation only if user interacts with the input
                var validationEnabled = scope.validationEnabled || scope.$root.validationEnabled;
                if (validationEnabled && !ngModel.$pristine) {
                  //broadcasting from root scope element validity
                  scope.$root.$broadcast(attrs.name, {errors: ngModel.$error});
                }
              });
            }
          };
        });
