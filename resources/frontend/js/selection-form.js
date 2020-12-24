$(function() {
	initSelectionForm();
});

function initSelectionForm() {
	let $selectAll = $(".select-all");
	$selectAll.on("click", function() {
		let $this = $(this);
		let checked = $this.prop("checked");
		$this.closest(".selection-form").find(" input[type=checkbox]")
			.prop("checked", checked).trigger("change");
	});

	$('.selection-form table tbody tr').on("click", function(event) {
		if (event.target.type !== 'checkbox') {
			$(':checkbox', this).trigger('click');
		}
	});

	$(".selection-form tbody input[type=checkbox]").on("change", function() {
		$(this).closest("tr").toggleClass("selected", $(this).is(":checked"));

		if ($(this).attr("name") === "groups") {
			let groupId = $(this).attr("data-group-id");
			let checked = $(this).prop("checked");
			$("input[data-group='" + groupId + "']").prop("checked", checked).trigger("change");
		}
	});

	$selectAll.prop("checked", "");
	$(".selection-form input[type=checkbox]").prop("checked", false).trigger("change");
}