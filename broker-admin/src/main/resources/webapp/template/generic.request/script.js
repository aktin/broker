
// executed a single time the template form has been injected
function initializeForm(){
	$('#new_request input[name="mediatype"]').val('*/*');
}

// build request XML from form data. use the given requestId
function compileForm(requestId){
	return $('#new_request textarea[name="content"]').val();
}

function getFormMediaType(){
	return $('#new_request input[name="mediatype"]').val();
}


// executed to validate the form (e.g. when submit button is pressed)
function validateForm(){
	// validate request syntax
	var text = $('#new_request input[name="mediatype"]').val();
	if( text == '' || text == '*/*' ){
		return false;
	}
	return true;
}

function fillForm(data, contentType, id){
	$('#new_request textarea[name="content"]').val(data);
	$('#new_request input[name="mediatype"]').val(contentType);
}


