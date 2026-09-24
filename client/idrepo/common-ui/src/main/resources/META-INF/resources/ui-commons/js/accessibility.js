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
