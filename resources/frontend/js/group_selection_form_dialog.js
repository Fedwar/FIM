let actionType;
let itemType;
let itemKey;
let itemName;
let groups = new Map();

$(function() {
    configureSelectAllEvents();
});

function configureSelectAllEvents() {
    let $selectAllGroups = $("#select-all-groups");
    $selectAllGroups.on("click", (function() {
        let $this = $(this);
        let checked = $this.prop("checked");
        $this.closest(".selection-form").find(" input[type=checkbox]")
            .prop("checked", checked).trigger("change");
    }));
    $selectAllGroups.prop("checked", "");
}

function createGroupSelectionFormDialog(actionTypeParam, itemTypeParam, itemKeyParam, itemNameParam, groupsParam,
                                        formTitle, confirmDialogTitle, assignItemTitle) {
    initVariables(actionTypeParam, itemTypeParam, itemKeyParam, itemNameParam, groupsParam);
    showAvailableGroups();
    $("#group-selection-form-dialog-label").text(formTitle);
    $("#start-assigning-button").on("click", function () {
        createGroupSelectionFormConfirmDialog(confirmDialogTitle, assignItemTitle);
    });
    $("#group-selection-form-dialog").modal();
}

function initVariables(actionTypeParam, itemTypeParam, itemKeyParam, itemNameParam, groupsParam) {
    actionType = actionTypeParam;
    itemType = itemTypeParam;
    itemKey = itemKeyParam;
    itemName = itemNameParam;
    groups.clear();
    if (groupsParam !== "[]") {
        groupsParam.replace("[", "").replace("]", "")
            .split(", ").forEach(function (group) {
            let groupEntry = group.split("=");
            groups.set(groupEntry[0], groupEntry[1]);
        });
    }
}

function showAvailableGroups() {
    $("#groups-body").empty();
    if (groups.size !== 0) {
        showGroupRows();
        $("#select-all-groups-row").show();
        $("#no-available-groups-row").hide();
        $('#start-assigning-button').prop("disabled", false);
    } else {
        $("#select-all-groups-row").hide();
        $("#no-available-groups-row").show();
        $('#start-assigning-button').prop("disabled", true);
    }
    if (itemType === "vehicle")
        changeInputTypeToRadio();
    initSelectionForm();
}

function showGroupRows() {
    groups.forEach(function (value, key) {
        let groupRowHtmlContent = '' +
            '<tr>' +
                '<td style="width: 30px">' +
                    '<input type="checkbox" data-group-id=' + value + ' name="available-groups" value=' + key + '>' +
                '</td>' +
                '<td class="group-name text-left">' + value + '</td>' +
            '</tr>';
        $("#groups-body").append(groupRowHtmlContent);
    });
}

function changeInputTypeToRadio() {
    $("#group-selection-form tbody input[name=available-groups]")
        .prop("type", "radio");
    $('.selection-form table tbody tr').on("click", function(event) {
        if (event.target.type !== 'radio')
            $(":radio", this).trigger("click");
    });
    $(".selection-form input[type=radio]").on("click", function() {
        $(".selection-form table tbody tr").removeClass("selected");
        $(this).closest("tr").addClass("selected");
    });
    $("#select-all-groups-row").hide();
}

function createGroupSelectionFormConfirmDialog(confirmDialogTitle, assignItemTitle) {
    $("#group-selection-form-confirm-dialog-label").text(confirmDialogTitle);
    $("#modal-assign-item-title").text(assignItemTitle);
    $("#modal-assign-item").text(itemName);
    $("#group-selection-form-confirm-dialog").modal();
    let selectedGroups = '';
    let $groupRows = $("#group-selection-form tbody input[name=available-groups]");
    $groupRows.each(function() {
        if (this.parentElement.parentElement.classList.contains("selected")) {
            selectedGroups += this.parentElement.parentElement
                .getElementsByClassName("group-name")[0].innerText + ', ';
        }
    });
    selectedGroups = selectedGroups.slice(0, -2);
    $("#modal-groups").text(selectedGroups);
}

function confirmAction() {
    let groups = $("#group-selection-form").serialize()
        .split("&").join("").split("available-groups=").slice(1);
    $("#group-selection-form-dialog").modal('hide');
    $.ajax({
        type: "POST",
        url: "/groups/" + itemKey + "/" + actionType + "-" + itemType,
        data: JSON.stringify(groups)
    }).then(function () {
        let relocation = window.location.href.replace("start-installation","");
        if (window.location.href !== relocation) {
            window.location.href = relocation
        } else {
            window.location.reload();
        }
    });
}