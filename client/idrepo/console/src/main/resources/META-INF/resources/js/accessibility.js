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

$(document).ready(function () {

  var locationDomain = window.location.origin + '/' + window.location.pathname.split('/')[1];

  var initAssetsManager = function () {
    var AssetsManager = {};

    var createLink = function (id, url) {
      if (!$('link#' + id).length && !$('link[href="' + url + '"').length) {
        var link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = url;
        link.type = "text/css";
        $('head').append(link);
      }
    };

    var createScript = function (id, url) {
      if (!$('script#' + id).length && !$('script[src="' + url + '"').length) {
        var script = document.createElement('script');
        script.src = url;
        $('body').append(script);
      }
    };

    AssetsManager.checkAlreadyLoaded = function (url, type) {
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

    var removeLoaded = function (url, type) {
      var tag = (type === 'js') ? 'script' : ((type === 'css') ? 'link' : '');
      if (AssetsManager.checkAlreadyLoaded(url, type)) {
        $(tag + '[href~="' + url + '"]').remove();
      }
    };

    AssetsManager.inject = function (id, url, type) {
      switch (type) {
        case 'js':
          createScript(id, url);
          break;

        case 'css':
          createLink(id, url);
          break;

        default:
          break;
      }
    };

    AssetsManager.remove = function (url, type) {
      removeLoaded(url, type);
    };

    return AssetsManager;
  };

  var initAccessibilityController = function () {
    AccessibilityController = {};

    var AssetsManager = initAssetsManager();

    var fontSizeFiles = [
      locationDomain + '/css/accessibility/accessibilityFont.css'
    ];

    var darkThemeFiles = [
      locationDomain + '/css/AdminLTE_skins/skin-blue.css',
      locationDomain + '/css/accessibility/accessibilityHC.css'
    ];

    var darkThemeMainClass = 'skin-blue';
    var defaultThemeMainClass = 'skin-green-light';

    var doSwitch = function (check, files) {
      if (!check) {
        for (var i = 0; i < files.length; i++) {
          AssetsManager.remove(files[i], 'css');
        }
      } else {
        for (var i = 0; i < files.length; i++) {
          AssetsManager.inject('theme_css_' + i, files[i], 'css');
        }
      }
    };

    var doSwitchTheme = function (check, files) {
      doSwitch(check, files);

      if ($('body').hasClass(defaultThemeMainClass) && check) {
        $('body').removeClass(defaultThemeMainClass).addClass(darkThemeMainClass);
      } else {
        $('body').removeClass(darkThemeMainClass).addClass(defaultThemeMainClass);
      }
    };

    var savePreference = function (key, value) {
      window.localStorage.setItem(key, value);
    };

    var getPreference = function (key) {
      var storageValue = window.localStorage.getItem(key);
      if (storageValue === null) {
        savePreference(key, 'false');
      }
      return storageValue === 'true';
    };

    AccessibilityController.FONT_SIZE_PREF = 'font_size_pref';
    AccessibilityController.HC_THEME_PREF = 'hc_theme_pref';

    var isIncreasedFont = getPreference(AccessibilityController.FONT_SIZE_PREF);
    var isHighContrast = getPreference(AccessibilityController.HC_THEME_PREF);

    AccessibilityController.checkPref = function (pref) {
      switch (pref) {
        case AccessibilityController.FONT_SIZE_PREF:
          doSwitch(isIncreasedFont, fontSizeFiles);
          break;

        case AccessibilityController.HC_THEME_PREF:
          doSwitchTheme(isHighContrast, darkThemeFiles);
          break;

        default:
          break;
      }
    };

    AccessibilityController.switchIncreasedFont = function () {
      isIncreasedFont = !isIncreasedFont;
      doSwitch(isIncreasedFont, fontSizeFiles);
      savePreference(AccessibilityController.FONT_SIZE_PREF, isIncreasedFont);
    };

    AccessibilityController.switchTheme = function () {
      isHighContrast = !isHighContrast;
      doSwitchTheme(isHighContrast, darkThemeFiles);
      savePreference(AccessibilityController.HC_THEME_PREF, isHighContrast);
    };

    return AccessibilityController;
  };

  var AccessibilityController = initAccessibilityController();

  AccessibilityController.checkPref(AccessibilityController.FONT_SIZE_PREF);
  AccessibilityController.checkPref(AccessibilityController.HC_THEME_PREF);

  $('#change_contrast').off('click.acc_hc');
  $('#change_contrast').on('click.acc_hc', function () {
    AccessibilityController.switchTheme();
    return false;
  });

  $('#change_contrast').off('keydown.key_acc_hc keypress.key_acc_hc');
  $('#change_contrast').on('keydown.key_acc_hc keypress.key_acc_hc', function (event) {
    // check "enter" key pressed
    if (event.which === 13) {
      AccessibilityController.switchTheme();

      event.preventDefault();
      return false;
    }
  });

  $('#change_fontSize').off('click.acc_f');
  $('#change_fontSize').on('click.acc_f', function () {
    AccessibilityController.switchIncreasedFont();
    return false;
  });

  $('#change_fontSize').off('keydown.key_acc_f keypress.key_acc_f');
  $('#change_fontSize').on('keydown.key_acc_f keypress.key_acc_f', function (event) {
    // check "enter" key pressed
    if (event.which === 13) {
      AccessibilityController.switchIncreasedFont();

      event.preventDefault();
      return false;
    }
  });

  $('body').off('keydown.acc_binding keypress.acc_binding');
  $('body').on('keydown.acc_binding keypress.acc_binding', function (event) {
    // alt - shift - F
    // alt - shift - H
    if (event.altKey && event.shiftKey) {
      if (event.keyCode === 72) {
        AccessibilityController.switchTheme();
      } else if (event.keyCode === 70) {
        AccessibilityController.switchIncreasedFont();
      }
      event.preventDefault();
    }
  });

});
