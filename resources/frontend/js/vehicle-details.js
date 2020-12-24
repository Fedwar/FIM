$(function() {

    $('#editNameDialog').on('show.bs.modal', function (event) {
        $("#editVehicleNameInput").val($("#vehicle-name").text());
    })

	$.ajaxSetup({ cache: false });
	
	$('.start-installation table tbody tr').on("click", function(event) {
		if (event.target.type !== 'radio') {
			$(':radio', this).trigger('click');
		}
	});
	
	$(".start-installation tbody input[type=radio]").on("change", function() {
		$(".start-installation tbody input[type=radio]").each(function() {
			$(this).closest("tr").toggleClass("selected", $(this).is(":checked"))
		});
	});
	
	setTimeout(updateTaskSnippet, 10 * 1000);
});

function updateTaskSnippet() {
	let taskSnippet = $('#tasks-snippet');
	let from = taskSnippet.data('url');
	taskSnippet.load(from, function(responseText, textStatus) {
		if (textStatus === 'success')
			setTimeout(updateTaskSnippet, 10 * 1000);
	});
}

function editVehicleName(event) {
    event.preventDefault();
	let $modal = $('#editNameDialog')
	let newName = $("#editVehicleNameInput").val();
	if (newName === "")
		newName = "null";
	$.get(location.href + '/edit-name/' + newName
	    , function(response) {
	        $modal.modal('hide');
        	autoUpdate();
        }
	);

}

function unassign(vehicleId) {
    let data = [];
    data.push(vehicleId);
    $.post("/groups/remove-vehicles", JSON.stringify(data), function() {
        window.location.reload();
    });
}

