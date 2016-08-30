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
        .directive('fileInput', function () {
          return {
            restrict: 'A',
            link: function ($scope, element, attrs) {
              var previewImgComposite;
              if ($scope.previewImg) {
                previewImgComposite = "data:image/png;base64," + $scope.previewImg;
              } else
                previewImgComposite = null;
              $(element).fileinput({
                showUpload: false,
                showCaption: false,
                showCancel: false,
                showClose: true,
                showRemove: false,
                fileActionSettings: {'showZoom': false, indicatorNew: '', 'removeTitle': 'boh'},
                removeClass: "btn btn-default",
                browseClass: "btn btn-default",
                browseLabel: '',
                dragIcon: '',
                browseIcon: '',
                initialPreviewAsData: true,
                overwriteInitial: true,
//                maxFileCount: 1,
//                'previewFileType': 'any',
                initialPreview: [
                  previewImgComposite
                ],
                'maxFileSize': 5120
              });
            }
          };
        });
        