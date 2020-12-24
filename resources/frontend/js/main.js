$(function() {
    displaySidebarIfItHasElements();
    $(window).on("click", hideUserDropDownMenuItems);
});

function displaySidebarIfItHasElements() {
    let $sidebar = $(".sidebar");
    if ($sidebar.children().length === 0) {
        $sidebar.css("display", "none");
    } else {
        $("main").addClass("with-sidebar");
    }
}

function hideUserDropDownMenuItems() {
    $("#user-drop-down-menu-items").hide();
}

function switchUserDropDownMenuItemsDisplay() {
    $("#user-drop-down-menu-items").toggle();
}