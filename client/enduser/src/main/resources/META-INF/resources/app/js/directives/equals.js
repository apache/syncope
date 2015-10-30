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

angular.module('self')
        .directive('equals', function () {
          return {
            restrict: 'A',
            require: '?ngModel',
            link: function (scope, elem, attrs, ngModel) {
              if (!ngModel)
                return; // do nothing if no ng-model

              // watch own value and re-validate on change
              scope.$watch(attrs.ngModel, function () {
                validate();
              });

              // observe the other value and re-validate on change
              attrs.$observe('equals', function (val) {
                validate();
              });

              var validate = function () {
                // values
                var val1 = ngModel.$viewValue;
                var val2 = attrs.equals;

                // set validity
                ngModel.$setValidity('equals', !val1 || !val2 || val1 === val2);
              };
            }
          };
        });