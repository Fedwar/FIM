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
    $(".device-group").each(function(i, obj) {
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
    collapseAllState = ($(".device-group").size() == $(".device-group.collapsed").size())
    $('#expand-all').toggleClass('collapsed', collapseAllState);
}

function toggleAll() {
    $expandAll = $('#expand-all')
    collapseAllState = !$expandAll.hasClass("collapsed")
    $expandAll.toggleClass("collapsed", collapseAllState);
    $('.toggle-tag').each(function(){toggle(this, collapseAllState);});
}