
// executed a single time the template form has been injected
function initializeForm(){
	$('#new_request input[name="text"]').val('test');
}

// build request XML from form data. use the given requestId
function compileForm(requestId){
	return $('#new_request input[name="text"]').val();
}

function getFormMediaType(){
	return 'application/x.test.request';
}


// executed to validate the form (e.g. when submit button is pressed)
function validateForm(){
	// validate request syntax
	var text = $('#new_request input[name="text"]').val();
	if( text == '' ){
		return false;
	}
	return true;
}

function fillForm(data, contentType, id){
	$('#new_request input[name="text"]').val(data);
}


