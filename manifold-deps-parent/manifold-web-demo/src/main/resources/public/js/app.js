$(document)
    .keyup('input', function (e) {
        if (e.keyCode === 27) {
            $(e.target).trigger('resetEscape');
        }
    })
    .click(function (e) {
        if ($('#todo-edit').length > 0 && !$(e.target).is('#todo-edit')) {
            $("#edit-form").trigger('submit');
        }
    });

Intercooler.ready(function () {
    $("[autofocus]:last").focus();
});