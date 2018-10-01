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
        .factory("AssetsManager", ['$q', function ($q) {
            var assetsManager = {};

            assetsManager.accessibilityCssId = "access_css";

            var createLink = function (id, url, deferred) {
              if (!angular.element('link[href="' + url + '"').length) {
                var link = document.createElement('link');
                link.id = id;
                link.rel = 'stylesheet';
                link.href = url;
                link.onload = deferred.resolve;
                link.onerror = deferred.reject;

                var themeElems = angular.element('[id*="' + assetsManager.accessibilityCssId + '"]');
                if (themeElems.length) {
                  angular.element(themeElems[0]).before(link);
                } else {
                  angular.element('head').append(link);
                }
              }
            };

            var createScript = function (id, url, deferred) {
              if (!angular.element('script[src="' + url + '"').length) {
                var script = document.createElement('script');
                script.id = id;
                script.src = url;
                script.onload = deferred.resolve;
                script.onerror = deferred.reject;
                angular.element('body').append(script);
              }
            };

            assetsManager.checkAlreadyLoaded = function (url, type) {
              var elems = (type === 'css') ? document.styleSheets : ((type === 'js') ? document.scripts : '');
              var attr = (type === 'js') ? 'src' : ((type === 'css') ? 'href' : 'none');
              for (var i in elems) {
                var attrUrl = elems[i][attr] || "";
                var assetName = attrUrl.split("/").slice(-1).join();
                if (attrUrl !== ""
                        && (assetName === url.split("/").slice(-1).join() || assetName === url)) {
                  return true;
                }
              }
              return false;
            };

            var checkLoaded = function (url, deferred, tries, type) {
              if (assetsManager.checkAlreadyLoaded(url, type)) {
                deferred.resolve();
                return;
              }
              tries++;
              setTimeout(function () {
                checkLoaded(url, deferred, tries, type);
              }, 50);
            };

            var removeLoaded = function (url, type) {
              var tag = (type === 'js') ? 'script' : ((type === 'css') ? 'link' : '');
              if (assetsManager.checkAlreadyLoaded(url, type)) {
                $(tag + '[href~="' + url + '"]').remove();
              }
            };

            assetsManager.inject = function (id, url, type) {
              var tries = 0,
                      deferred = $q.defer();

              switch (type) {
                case 'js':
                  createScript(id, url, deferred);
                  break;

                case 'css':
                  createLink(id, url, deferred);
                  break;

                default:
                  break;
              }
              checkLoaded(url, deferred, tries, type);

              return deferred.promise;
            };

            assetsManager.remove = function (url, type) {
              removeLoaded(url, type);
            };

            return assetsManager;
          }]);
