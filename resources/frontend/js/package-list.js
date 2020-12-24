let packageSearch = $("#package-search");
packageSearch.on('input change', filterPackageList);

let cntrlIsPressed =  false;
let shiftIsPressed =  false;
let lastSelected;
let selectedSet = new Set();

$(function() {
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
});

function filterPackageList() {
	let searchTerm = packageSearch.val().toLowerCase();
    packageSearch.toggleClass("active", searchTerm !== "");
	
	$(".package-category").each(function() {		
		$(this).nextUntil(".package-category").each(function() {
			let text = $(this).find(".package-name").text();
			if (text.toLowerCase().indexOf(searchTerm) >= 0) {
				$(this).show();
			}
			else {
				$(this).hide();
			}
		});
	});
}

function dragPackage(event) {
    let packageRow = $(event.target).closest("tr");
    if (!packageRow.hasClass("selected-package")) {
        selectSingleRow(packageRow);
    }
     event.dataTransfer.setData('text', findPackageId(packageRow));
}

function allowDrop(event) {
    event.preventDefault();
}

function createAssignGroupDialog(event) {
    event.preventDefault();
    $("#assignPackagesDialog").modal();

    let data = '';
    selectedSet.forEach(function(value) {
        data += $("#" + value).text() + ', ';
    });
    data = data.slice(0, -2);
    $("#modalPackages").text(data);

    let modalGroup = $("#modalGroup");
    modalGroup.attr("groupId", event.target.id);
    modalGroup.text(event.target.text);
}

function assignGroup() {
    let groupId = $("#modalGroup").attr("groupId");
    let data = [];
    selectedSet.forEach(function(value) {
      data.push(value);
    });

    $.ajax({
        type: "POST",
        url: "/groups/" + groupId + "/assign-packages",
        data: JSON.stringify(data),
        success: function () {
            autoUpdate("packages-snippet");
        }
    })
}

function packageClick(packageRow) {
    if (!cntrlIsPressed && !shiftIsPressed ) {
        selectSingleRow($(packageRow));
    }
}

function packageMouseDown(packageRow) {
    let $packageRow = $(packageRow);
    if (cntrlIsPressed) {
        invertRow($packageRow)
    } else if (shiftIsPressed) {
        clearRows();
        markRange($packageRow);
    } else {
        if (!$(packageRow).hasClass("selected-package")) {
            selectSingleRow($packageRow);
        }
    }
}

function selectSingleRow($packageRow) {
    clearRows();
    markRow($packageRow);
}

function markRow($packageRow) {
    $packageRow.addClass("selected-package");
    let packageId = findPackageId($packageRow);
    selectedSet.add(packageId);
    lastSelected = packageId;
}

function clearRows() {
    $(".package").removeClass("selected-package");
    selectedSet.clear();
}

function invertRow($packageRow) {
    if ($packageRow.hasClass("selected-package")) {
        $packageRow.removeClass("selected-package");
        selectedSet.delete(findPackageId($packageRow));
    } else {
        markRow($packageRow);
    }
}

function findPackageId($packageRow) {
    return $packageRow.find(".package-name").find("a").attr("id")
}

function markRange($packageRow) {
    let startSelecting = false;
    let stopSelecting = false;
    let selected = findPackageId($packageRow);
    clearRows();
    $(".package").each(function(){
        $packageRow = $(this);
        let id = findPackageId($packageRow);
        if (startSelecting && !stopSelecting) {
            $packageRow.addClass("selected-package");
            selectedSet.add(id);
            stopSelecting = id === selected || id === lastSelected;
        } else {
            startSelecting = id === selected || id === lastSelected;
            if (startSelecting) {
                $packageRow.addClass("selected-package");
                selectedSet.add(id);
            }
        }
    });
}

function highlightSelected() {
    selectedSet.forEach(function(item) {
        $('#' + item).closest("tr").addClass("selected-package");
    });
}

function deletePackage(key, message) {
    if (window.confirm(message)) {
        $.ajax({
            type: "GET",
            url: "packages/delete?key=" + key,
            success: function () {
                autoUpdate("packages-snippet");
            }
        })
    }
}

let collapsedMap = new Map();
let collapseAllState = true;

function restoreCollapsedState() {
    collapsedMap.forEach(function (value, key) {
        let toggleRows = $("tr[data-groupid='" + key + "']");
        toggleRows.toggleClass("collapsed", value);
    });
    $('#expand-all').toggleClass('collapsed', collapseAllState);
}

function toggleGroup(sender, state) {
    toggle(sender, state)
    checkCollapseAll();
}

function initCollapsedState() {
    $(".package-category").each(function(i, obj) {
        $this = $(this)
        collapsedMap.set( $this.data("groupid"), $this.hasClass("collapsed"));
    });
}

function toggle(sender, state) {
    if (sender.nodeName == "TR")
        var groupHeaderRow = $(sender);
    else
        var groupHeaderRow = $(sender).parents("tr");
    let groupId = groupHeaderRow.data("groupid");
    let toggleRows = $("tr[data-groupid='" + groupId + "']");

    if (state !== undefined) {
        toggleRows.toggleClass("collapsed", state);
    } else {
        toggleRows.toggleClass("collapsed");
    }

    collapsedMap.set(groupId, groupHeaderRow.hasClass("collapsed"));
}

function checkCollapseAll() {
    collapseAllState = ($(".package-category").size() == $(".package-category.collapsed").size())
    $('#expand-all').toggleClass('collapsed', collapseAllState);
}

function toggleAll() {
    $expandAll = $('#expand-all')
    collapseAllState = !$expandAll.hasClass("collapsed")
    $expandAll.toggleClass("collapsed", collapseAllState);
    $('.toggle-tag').each(function(){toggle(this, collapseAllState);});
}