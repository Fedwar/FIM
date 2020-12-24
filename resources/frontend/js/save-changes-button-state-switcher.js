let startFormData;

$(function() {
    initStartFormState();
    setSaveChangesButtonState();
});

function initStartFormState() {
    startFormData = $('form').serialize();
}

function setSaveChangesButtonState() {
    if (haveAnyChangesBeenMade())
        enableSaveChangesButton();
    else
        disableSaveChangesButton();
}

function haveAnyChangesBeenMade() {
    return startFormData !== $('form').serialize();
}

function disableSaveChangesButton() {
    $(".save-changes-button").prop("disabled", true);
}

function enableSaveChangesButton() {
    $(".save-changes-button").prop("disabled", false);
}