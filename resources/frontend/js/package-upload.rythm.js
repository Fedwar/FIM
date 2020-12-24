var progress = $(".progress-bar");
var viewBeforeUpload = $("#before-upload");
var viewDuringUpload = $("#during-upload");
var viewOnError = $("#on-error");
var activityLabel = $(".activity-label");
var serverError = $("#server-error");

$(document).ready(function() {
	viewDuringUpload.hide();
	viewOnError.hide();
	viewBeforeUpload.show();
	$("input[type=file]").change(submitForm);
});

function setProgress(percent) {
	progress.css("width", percent + "%");
}

function showError(error) {
	serverError.text(error);
	viewBeforeUpload.show();
	viewDuringUpload.hide();
	viewOnError.show();
}

function submitForm() {
	$('#package-name').val($('#package').val());
	var filename = $("#package").val();
	$("#file-name").text(filename.match(/([^\\/]+)$/)[1]);
	
	$('form').ajaxSubmit({
	    beforeSend: function() {
	    	viewBeforeUpload.hide();
	    	viewOnError.hide();
	    	viewDuringUpload.show();
	    	setProgress(0);
	    },
	    uploadProgress: function(event, position, total, percentComplete) {
	    	var lbl = "@("package_upload_processing".i18n())";

	    	if (percentComplete == 100) {
	    		activityLabel.text(lbl);
	    	}
	    	
    		var percent = Math.floor(percentComplete * 0.7);
    		setProgress(percent);
	    },
	    success: function(data, status, xhr) {
	    	setProgress(100);
	    	var lbl = "@("package_upload_success".i18n())";
	    	activityLabel.text(lbl);

	    	window.setTimeout(function() {
	    		var packageLocation = xhr.getResponseHeader("Location");
	    		window.location.replace(packageLocation);
	    	}, 3000);
	    },
		error: function(response) {
			var error = response.responseText;
			
			if (response.responseJSON)
				error = response.responseJSON.errorMessage;
			
			showError(error);
		}
	}); 
}