function confirmPackageDeleting(packageKey) {
    if (window.confirm('@i18n("packages_delete_question")'))
        window.location.href = "/packages/delete?key=" + packageKey;
}

function createStartInstallationDialog() {
    $("#start-installation-dialog").modal();
    let vehicles = '';
    $(".selection-form tbody input[name=vehicles]").each(function() {
        if (this.parentElement.parentElement.classList.contains("selected"))
            vehicles += this.parentElement.innerText + ', ';
    });
    vehicles = vehicles.slice(0, -2);
    $("#modal-vehicles").text(vehicles);
}

function startInstallation() {
    $('#packageInstallForm').submit();
}