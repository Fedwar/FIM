let searchField = $("#vehicle-search");
searchField.on('input change', filterVehicleList);
let cntrlIsPressed =  false;
let shiftIsPressed =  false;
let lastSelected;
let selectedSet = new Set()
let searchVehiclesBy = "name";;

var groupEditValidator;
var groupAddValidator;

$(function() {
    filterVehicleList();

    document.addEventListener("keydown", function(event){
        if(event.which===17)
            cntrlIsPressed = true;
        if(event.which===16)
            shiftIsPressed = true;
    });

    document.addEventListener("keyup", function(){
        cntrlIsPressed = false;
        shiftIsPressed = false;
    });

    document.getElementById("deleteGroupButton").addEventListener("click", function() {
        $.ajax({
            url: "/groups/" + $(this).data('groupId'),
            type: 'DELETE',
            success: groupChanged,
            error: function (response, status, error) {
                showModalErrorMessage(response.responseJSON)
            }
        })
    });

    groupAddValidator = $("form#addGroupForm").validate({
        lang: '@i18n("local")',
        rules: {
            newGroupNameInput: 'required',
            newGroupPathInput: 'required'
        }
    });
    groupEditValidator = $("form#editGroupForm").validate({
        lang: '@i18n("local")',
        rules: {
            editGroupNameInput: 'required',
            editGroupPathInput: 'required'
        }
    });

});

function changeSearchBy(searchVehiclesByParam) {
    searchVehiclesBy = searchVehiclesByParam;
    let placeholderValue = '@i18n("vehicle_list_find_by_name")';
    if (searchVehiclesBy === "uic")
        placeholderValue = '@i18n("vehicle_list_find_by_id")';
    if (searchVehiclesBy === "additional-uic")
        placeholderValue = '@i18n("vehicle_list_find_by_uic")';
    searchField.attr("placeholder", placeholderValue);
    autoUpdate();
}

function filterVehicleList() {
    let searchTerm = searchField.val().toLowerCase();
    searchField.toggleClass("active", searchTerm !== "");

	$(".vehicle-category").each(function() {
        let visibleRows = 0;
		$(this).find(".vehicle").each(function() {
            let text = $(this).find(".vehicle-name").attr(searchVehiclesBy);
            if (text.toLowerCase().indexOf(searchTerm) >= 0) {
				$(this).show();
				visibleRows++;
			}
			else {
				$(this).hide();
			}
		});
		$(this).find(".vehicle-counter").text(visibleRows);
	});
}

function addGroup() {
    if (groupAddValidator.form()) {
        $.ajax({
            type: "POST",
            url: "/groups",
            data: JSON.stringify({
                "name": $('#newGroupNameInput').val(),
                "dir": $('#newGroupPathInput').val(),
            }),
            success: function () {
                $('#newGroupNameInput').val("");
                $('#newGroupPathInput').val("");
                groupChanged()
            },
            error: function (response, status, error) {
                showModalErrorMessage(response.responseJSON)
            }
        })
    }
}

function editGroup() {
    if (groupEditValidator.form()) {
        $.ajax({
            type: "POST",
            url: "/groups/" + $("#editGroupButton").data('groupId'),
            data: JSON.stringify({
                "id": $("#editGroupButton").data('groupId'),
                "name": $('#editGroupNameInput').val(),
                "dir": $('#editGroupPathInput').val(),
            }),
            success: groupChanged,
            error: function (response, status, error) {
                showModalErrorMessage(response.responseJSON)
            }
        })
    }
}

function groupChanged(){
    window.location.reload()
}

$('.modal').on('show.bs.modal', function (event) {
    hideModalMessage();
});

$('#newGroupDialog').on('show.bs.modal', function (event) {
    let $modal = $(this);
    $modal.find("form").trigger('reset');
    $modal.find('input').removeClass('error');
    groupAddValidator.resetForm();
});

$('#editGroupDialog').on('show.bs.modal', function (event) {
    let button = $(event.relatedTarget);
    let groupName = button.data('groupname');
    let groupDir = button.data('groupdir');
    let groupId = button.data('groupid');
    button.data('groupautosync');
    let $modal = $(this);
    $modal.find('.modal-body #editGroupNameInput').val(groupName);
    $modal.find('.modal-body #editGroupPathInput').val(groupDir);
    $modal.find('.modal-footer #editGroupButton').data('groupId', groupId);
    $modal.find('input').removeClass('error');
    groupEditValidator.resetForm();
});

$('#deleteGroupDialog').on('show.bs.modal', function (event) {
    let button = $(event.relatedTarget);
    let groupName = button.data('groupname');
    let groupId = button.data('groupid');
    let modal = $(this);
    let message = '@i18n("group_delete_dialog_message")';
    modal.find('.modal-body #deleteGroupSpan').html(message.replace('{0}', groupName));
    modal.find('.modal-footer #deleteGroupButton').data('groupId', groupId);
});

$('.modal').on('shown.bs.modal', function (event) {
    let modal = $(this);
    let $input = modal.find('input:text:visible:first');
    if ($input.length > 0) {
        $input.focus();
    } else {
        modal.find('.btn-primary').focus();
    }
});


$("#newGroupNameInput,#newGroupPathInput").on("keypress", function(e) {
    if (e["keyCode"] === 13) {
        $('#newGroupDialog').modal('hide');
        addGroup();
    }
});

$("#editGroupNameInput,#editGroupPathInput").on("keypress", function(e) {
    if (e["keyCode"] === 13) {
        $('#editGroupDialog').modal('hide');
        editGroup();
    }
});

function confirmCancel() {
    return window.confirm('@("vehicle_cancel_tasks_question".i18n())');
}

function dragVehicle(event) {
    let vehicleRow = $(event.target).closest("tr");
    if (!vehicleRow.hasClass("selected-vehicle")) {
        selectSingleRow(vehicleRow);
    }
    event.dataTransfer.setData('text', findVehicleId(vehicleRow));
}

function allowDrop(event) {
    event.preventDefault()
}

//function clearSelection(formId) {
//    $("#" + formId + " tbody input:checked").each(function() {
//        $(this).attr('checked','');
//    });
//}

let groupId;
let vehicleIds;

function assignSelectedVehicles(event) {
    event.preventDefault();

    vehicleIds = Array.from(selectedSet);
    let vehicleNames = [];
    selectedSet.forEach(function(value) {
        vehicleNames.push($("#" + value).text());
    });
    groupId = event.target.id;

    createAssignVehiclesConfirmDialog(event.target.text, vehicleNames);
}

function unssignSelectedVehicles(event) {
    event.preventDefault();

    vehicleIds = Array.from(selectedSet);
    let vehicleNames = [];
    selectedSet.forEach(function(value) {
        vehicleNames.push($("#" + value).text());
    });

    createUnssignVehiclesConfirmDialog(vehicleNames);
}

function assignPickedVehicles() {
    groupId = $("#vehicleGroup").val();
    vehicleIds = [];
    let vehicleNames = [];
    $("#assignVehiclesForm tbody input[name=vehicles]:checked").each(function() {
        vehicleIds.push($(this).val());
        vehicleNames.push($(this).parent().text());
    });

    createAssignVehiclesConfirmDialog($("#vehicleGroup option:selected").text(), vehicleNames);
}

function unassignPickedVehicles() {
    vehicleIds = [];
    let vehicleNames = [];
    $("#unassignVehiclesForm tbody input[name=vehicles]:checked").each(function() {
        vehicleIds.push($(this).val());
        vehicleNames.push($(this).parent().text());
    });

    createUnssignVehiclesConfirmDialog(vehicleNames);
}

function createAssignVehiclesConfirmDialog(groupName, vehicleNames) {
    $("#assignVehiclesConfirm").modal();
    $("#modalVehicles").text(vehicleNames.join(', '));
    $("#modalGroup").text(groupName);
}

function createUnssignVehiclesConfirmDialog(vehicleNames) {
    $("#unassignVehiclesConfirm").modal();
    $("#modalVehiclesForRemovingFromGroup").text(vehicleNames.join(', '));
}

function assignVehicles() {
    $.post("/groups/" + groupId + "/assign-vehicles", JSON.stringify(vehicleIds), groupChanged);
}

function unassignVehicles() {
    $.post("/groups/remove-vehicles", JSON.stringify(vehicleIds), groupChanged);
}

function vehicleClick(vehicleRow) {
    if (!cntrlIsPressed && !shiftIsPressed ) {
        selectSingleRow($(vehicleRow));
    }
}

function vehicleMouseDown(vehicleRow) {
    let $vehicleRow = $(vehicleRow);
    if (cntrlIsPressed) {
        invertRow($vehicleRow)
    } else if (shiftIsPressed) {
        clearRows();
        markRange($vehicleRow);
    } else {
        if (!$(vehicleRow).hasClass("selected-vehicle")) {
            selectSingleRow($vehicleRow);
        }
    }
}

function selectSingleRow($vehicleRow) {
    clearRows();
    markRow($vehicleRow);
}

function markRow($vehicleRow) {
    $vehicleRow.addClass("selected-vehicle");
    let vehicleId = findVehicleId($vehicleRow);
    selectedSet.add(vehicleId);
    lastSelected = vehicleId;
}

function clearRows() {
    $(".vehicle").removeClass("selected-vehicle");
    selectedSet.clear();
}

function invertRow($vehicleRow) {
    if ($vehicleRow.hasClass("selected-vehicle")) {
        $vehicleRow.removeClass("selected-vehicle");
        selectedSet.delete(findVehicleId($vehicleRow));
    } else {
        markRow($vehicleRow);
    }
}

function findVehicleId($vehicleRow) {
    return $vehicleRow.find(".vehicle-name").find("a").attr("id")
}

function markRange($vehicleRow) {
    let startSelecting = false;
    let stopSelecting = false;
    let selected = findVehicleId($vehicleRow);
    clearRows();
    $(".vehicle").each(function(){
        $vehicleRow = $(this);
        let id = findVehicleId($vehicleRow);
        if (startSelecting && !stopSelecting) {
            $vehicleRow.addClass("selected-vehicle");
            selectedSet.add(id);
            stopSelecting = id === selected || id === lastSelected;
        } else {
            startSelecting = id === selected || id === lastSelected;
            if (startSelecting) {
                $vehicleRow.addClass("selected-vehicle");
                selectedSet.add(id);
            }
        }
    });
}

function highlightSelected() {
    selectedSet.forEach(function(item) {
        $('#' + item).closest("tr").addClass("selected-vehicle");
    });
}

let isAfterUpdateSortingNeed = false;

function sortVehiclesByNames() {
    let sortIcon = $("#sort-vehicles-by-names-icon");
    if (sortIcon.hasClass("glyphicon-triangle-top")) {
        sortIcon.addClass("glyphicon-triangle-bottom");
        sortIcon.removeClass("glyphicon-triangle-top");
    } else {
        sortIcon.addClass("glyphicon-triangle-top");
        sortIcon.removeClass("glyphicon-triangle-bottom");
    }

    let elements = $(".vehicle");
    elements.each(function() {
        $(this).insertAfter(elements.last());
    });

    isAfterUpdateSortingNeed = !isAfterUpdateSortingNeed;
}

function sortVehiclesByNamesAfterUpdateIfNeed() {
    if (isAfterUpdateSortingNeed) {
        sortVehiclesByNames();
        isAfterUpdateSortingNeed = true;
    }
}