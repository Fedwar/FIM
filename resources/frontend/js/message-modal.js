function showModalSuccessMessage(message) {
    showModalMessage(message, "alert-success")
}

function showModalErrorMessage(message) {
    showModalMessage(message, "alert-danger")
}

function showModalMessage(message, type) {
    $modal = $(".modal-dialog:visible");
    $alert = $modal.find(".modalAlert");
    $alert.slideUp(400, function() {
        $alert.removeClass()
            .addClass( type )
            .addClass( "modalAlert" )
            .addClass( "alert" )
        $alert.text(message);
        $alert.slideDown(400);
    })
}

function hideModalMessage() {
    $alert = $(".modalAlert");
    $alert.hide();
}