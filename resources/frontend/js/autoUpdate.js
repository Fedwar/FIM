@args String containerId, boolean dragDetection = true

let isDrag = false;
var autoUpdateContainerId = '@containerId';
const AUTO_UPDATE_DELAY = 10 * 1000;
var autoUpdateTimer;
var autoUpdateEnabled=true;

function autoUpdate() {
    clearTimeout(autoUpdateTimer);
    if (autoUpdateEnabled && !isDrag) {
        if (typeof autoUpdateOverride === "function") {
            autoUpdateOverride();
        } else {
            defaultAutoUpdate()
        }
    }
    autoUpdateTimer = setTimeout(function(){autoUpdate()}, AUTO_UPDATE_DELAY);
}

function startAutoUpdateTimer() {
    autoUpdateTimer = setTimeout(function(){autoUpdate()}, AUTO_UPDATE_DELAY);
}

function disableAU() {
    autoUpdateEnabled = false;
}

function defaultAutoUpdate() {
    $autoUpdateContainer = $("#" + autoUpdateContainerId);
    let autoUpdateUrl = $autoUpdateContainer.data("url");
    $autoUpdateContainer.load(autoUpdateUrl, function(responseText, textStatus) {
        if (textStatus === "success")
            if (typeof afterUpdate === "function") {
               afterUpdate();
            }
    });
}

$(function() {
    $.ajaxSetup({ cache: false });
    startAutoUpdateTimer();
});

@if (dragDetection) {
    document.addEventListener("dragstart", function() {
      isDrag = true
    }, false);

    document.addEventListener("dragend", function() {
      isDrag = false
    }, false);
}
