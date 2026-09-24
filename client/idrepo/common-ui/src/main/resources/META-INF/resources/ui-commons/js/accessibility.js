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
$(function () {
    const locationDomain = window.location.origin + '/' + window.location.pathname.split('/')[1];
    const fontSizeCss = locationDomain + '/ui-commons/css/accessibility/accessibilityFont.css';
    const fontSizePreference = 'font_size_pref';

    let increasedFont = localStorage.getItem(fontSizePreference) === 'true';

    const switchFontSize = () => {
        const $stylesheet = $('link#font_size_css');

        if (increasedFont) {
            if (!$stylesheet.length) {
                $('<link>', {
                    id: 'font_size_css',
                    rel: 'stylesheet',
                    href: fontSizeCss
                }).appendTo('head');
            }
        } else {
            $stylesheet.remove();
        }

        localStorage.setItem(fontSizePreference, increasedFont);
    };

    const toggleFontSize = () => {
        increasedFont = !increasedFont;
        switchFontSize();
    };

    switchFontSize();

    $('#change_fontSize')
        .off('.acc_font')
        .on('click.acc_font', function () {
            toggleFontSize();
            return false;
        });
});
