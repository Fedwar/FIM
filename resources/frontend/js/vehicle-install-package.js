$(function() {
    $('#install-package-dialog').modal('show');
});

function createConfirmDialog() {
    let $selected = $('.selected');
    if ($selected.length !== 0) {
        let packageName = $selected.find('td')[1].textContent;
        $('#confirm-installation-dialog').modal('show');
        $("#modal-package").text(packageName);
    } else
        location.href = location.href.replace('install-package','installation-error');
}

function submitInstallation() {
    $('.start-installation').trigger('submit');
}