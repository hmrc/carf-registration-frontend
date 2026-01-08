// Find first ancestor of el with tagName
// or undefined if not found
function upTo(el, tagName) {
    tagName = tagName.toLowerCase();

    while (el && el.parentNode) {
        el = el.parentNode;
        if (el.tagName && el.tagName.toLowerCase() == tagName) {
            return el;
        }
    }

    return null;
}

var countrySelect = document.querySelector('select#country');
if (countrySelect !== null) {
    var options = countrySelect.querySelectorAll("option");
    for (var i = 0; i < options.length; i++) {
        var option = options[i];
        var dataText = option.getAttribute('data-text');
        if (dataText) {
            option.text = dataText;
        }
    }
    setTimeout(function(){
        HMRCAccessibleAutocomplete.enhanceSelectElement({
            defaultValue: '',
            selectElement: countrySelect,
            showAllValues: false,
            minLength: 1,
            autoselect: false,
            containerClassName: 'govuk-!-width-two-thirds',
            templates: {
                suggestion: function (suggestion) {
                    if (suggestion) {
                        return suggestion.split(':')[0];
                    }
                    return suggestion;
                },
                inputValue: function (suggestion) {
                    if (suggestion) {
                        return suggestion.split(':')[0];
                    }
                    return suggestion;
                }
            }
        });
    }, 100)
}
