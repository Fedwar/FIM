var $indicatorsList;
var $vehiclesList;
var $indicatorsParam;
var $vehiclesParam;

$(document).ready(function() {
    $indicatorsList = $("#indicatorsList");
    $vehiclesList = $("#vehiclesList");
    $indicatorsParam = $("#indicators");
    $vehiclesParam = $("#vehicles");

    $(".datepicker").datepicker({
        changeMonth: true,
        changeYear: true,
        dateFormat: 'dd.mm.yy'
    });
    $.datepicker.regional[ '@i18n("local")' ]

    $('.reportParameter').find("input").focus(showSelectOptions);

	$("#indicatorsList tbody input[type=checkbox]").change(function() {
	    refreshIndicatorsField();
	});

	$("#vehiclesList tbody input[type=checkbox]").change(function() {
        refreshVehiclesField();
	});

    $("#select-all-indicators").click(function() {
        $this = $(this);
        checked = $this.prop("checked");
        $this.closest(".selection-form").find(" input[type=checkbox]").prop("checked", checked).trigger("change");
    });

	refreshVehiclesField();
	refreshIndicatorsField();
	refreshReportParameters();
});

function refreshField($field, $checkBoxes) {
    let selected = '';
    $checkBoxes.each(function () {
        selected += $(this).val() + ',';
    });
    $field.val(selected)
}

function refreshIndicatorsField() {
    refreshField($indicatorsParam, $indicatorsList.find('.selected input') );
}

function refreshVehiclesField() {
    refreshField($vehiclesParam, $vehiclesList.find('.selected input[name="vehicles"]'));
}

function convertDate(date) {
    let dateparts = date.split(".");
    return dateparts[2] + "-" + dateparts[1] + "-" +  dateparts[0]
}

function downloadReport() {
    let reportType = $('#reportType').val();
    let rangeBy = $('#rangeBy').val();
    let startDate = $('#startDateInput').val();
    let endDate = $('#endDateInput').val();

    if (!isAllParametersFilled()) {
        alert('@("reports_fill_all_parameters".i18n())');
        return;
    }

    startDate = convertDate(startDate);
    endDate = convertDate(endDate);
    if ((isNaN(Date.parse(startDate))) || (isNaN(Date.parse(endDate)))) {
        alert('@("reports_wrong_date_format".i18n())');
        return;
    }

    let selectedVehicles = '';
    $('.selected').children().children('input[name="vehicles"]').each(function (i, el) {
        selectedVehicles += $(el).attr("id") + ',';
    });

    let ref =
        '/report/' + reportType + '/' + startDate + '/' + endDate + '/' + selectedVehicles + '/' + rangeBy;
    if (reportType === 'operation-data') {
        ref += '/';
        $indicatorsList.find('.selected input').each(function () {
            ref += $(this).val() + ',';
        });
    }
    location.href = escape(ref);

}

function isAllParametersFilled() {
    var result = true;
    $('.reportParameter:visible input').each(function(i, obj) {
        if (obj.value === '') {
            result = false;
            return;
        }
    });
    return result
}

function showSelectOptions() {
    var id = $(this).attr("id")

    $selectArea = $("#selectArea");
    $selectArea.fadeOut(200, function() {
        $('#vehiclesList').hide();
        $('#indicatorsList').hide();
        if (id == 'vehicles') {
            $('#vehiclesList').show();
        } else if (id == 'indicators') {
            $('#indicatorsList').show();
        }
        $selectArea.fadeIn(200);
    })
}

function refreshReportParameters() {
    var type = $('#reportType').val();
    $('#indicators').closest(".reportParameter").hide();
    if (type == 'operation-data') {
        $('#indicators').closest(".reportParameter").show();
    }
}

function showReportParameters() {

    $("#selectArea").fadeOut(200);

    $reportParameters = $("#reportParameters");
    $reportParameters.fadeOut(200, function() {
        refreshReportParameters();
        $reportParameters.fadeIn(200);
    })
}


