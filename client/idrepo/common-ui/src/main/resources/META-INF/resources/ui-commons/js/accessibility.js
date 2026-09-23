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

    switchFontSize();

    const toggleFontSize = () => {
        increasedFont = !increasedFont;
        switchFontSize();
    };

    $('#change_fontSize').off('.acc_font').on({
        'click.acc_font': function () {
            toggleFontSize();
            return false;
        },

        'keydown.acc_font': function (event) {
            if (event.key === 'Enter' || event.key === ' ') {
                toggleFontSize();
                event.preventDefault();
                return false;
            }
        }
    });

    $('body').off('.acc_binding').on('keydown.acc_binding', function (event) {
        if (event.altKey && event.shiftKey && event.key === 'F') {
            toggleFontSize();
            event.preventDefault();
        }
    });
});
