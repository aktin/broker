
function isRepeatingExecution(){
	return $('#x_qid').parent('fieldset').attr('id') == 'x_exec';
}
function switchExecutionForm(repeating){
		var target;
		if( repeating ){
			// switch to repeating
			target = $('#x_exec');
			$(this).text('Repeated execution (click to change)');
			// add validation rules
		}else{
			// switch to single
			target = $('#hidden_fields');
			$(this).text('Single execution (click to change)');
		}
		
		$('#x_intvl').prop('required',repeating);
		$('#x_qid').prop('required',repeating);
		$('#x_intvl').detach().appendTo(target);
		$('#x_qid').detach().appendTo(target);

}

// executed a single time the template form has been injected
function initializeForm(){
	$('#new_request input[name="scheduled"]').val(new Date().toDateInputValue());
	$('#new_request input[name="p_name"]').val('')
	$('#new_request input[name="p_email"]').val('');
	$('#new_request input[name="title"]').val('')
	$('#new_request input[name="x_ts"]').val(new Date().toDateInputValue()+"T00:00");

	// add button to set date
	$('#new_request input[name="reference"]').after($('<a>Last month</a>').click(function(){
		var d = new Date();
		d.setDate(1);
		console.log('Setting reference date',d);
		$('#new_request input[name="reference"]').val(d.toDateInputValue()+"T00:00");
	}));
	// switch between single/repeating executions
	$('#x_exec legend:first-child').css('cursor','pointer').click(function(){
		var rep = isRepeatingExecution();
		console.log("qid parent fieldset length", rep);
		switchExecutionForm(!rep);
	});

	// XML validation
	
}
function buildRequestDefinitionXml(requestId, fragment){
	// check for single or multiple execution
	var xml;
	if( isRepeatingExecution() ){
		xml = $.parseXML('<queryRequest xmlns="http://aktin.org/ns/exchange" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><id>'+requestId+'</id><reference/><scheduled/><query><title/><description/><principal><name/><organisation/><email/><phone/></principal><schedule xsi:type="repeatedExecution"><duration/><interval/><id/></schedule></query></queryRequest>');	
	}else{
		xml = $.parseXML('<queryRequest xmlns="http://aktin.org/ns/exchange" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><id>'+requestId+'</id><reference/><scheduled/><query><title/><description/><principal><name/><organisation/><email/><phone/></principal><schedule xsi:type="singleExecution"><duration/></schedule></query></queryRequest>');
	}
	$(xml).find('scheduled').text(localTimeToISO($('#new_request input[name="scheduled"]').val()));
	$(xml).find('reference').text(localTimeToISO($('#new_request input[name="reference"]').val()));
	var query = $(xml).find('query')[0];
	// use form elements
	$('#new_request *').filter(':input').each(function(){
		var jpath = $(this).data('jpath');
		if( jpath == '' )return;
		$(xml).find(jpath).text($(this).val());
	});
	// append XML fragment to query
	$(query).append($(fragment).find('*').eq(0));
	return xml;
}

// build request XML from form data. use the given requestId
function compileForm(requestId){
	var fragment = $.parseXML($('#new_request textarea[name="xml"]').val());
	var data = buildRequestDefinitionXml(requestId, fragment);
	return data;
}

function getFormMediaType(){
	return 'application/vnd.aktin.query.request+xml';
}


// executed to validate the form (e.g. when submit button is pressed)
function validateForm(){
	// perform html5 validation
	console.log('validating..');
	if( false == $('#new_request')[0].checkValidity() ){
		$('#new_request')[0].reportValidity();
		return false;
	}
	// validate request syntax
	var ta = $('#new_request textarea[name="xml"]')[0];
	try{
		var fragment = $.parseXML(ta.value);
		// validation successful since no error was thrown
		ta.setCustomValidity(''); // clear error message
		ta.reportValidity();
	}catch( e ){
		try{
			ta.setCustomValidity("No valid XML!")
			ta.reportValidity();
		}catch( e ){
			// HTML5 form validation reporting failed, display alert box
			alert('query definition not valid XML');
		}
		return false;
	}
	return true;
}

function fillForm(data, contentType, id){
	var xml = $.parseXML(data);
	
	// fill form elements with xml info
	$('#new_request *').filter(':input').each(function(){
		var jpath = $(this).data('jpath');
		if( jpath == '' )return;
		$(this).val( $(xml).find(jpath).text() );
	});

	// compile query contents
	var frag = '';
	var xs = new XMLSerializer();
	// start with first element following query/schedule
	var el = $(xml).find('query schedule')[0].nextElementSibling;
	for( ; el != null; el = el.nextElementSibling ){
		frag += xs.serializeToString(el);
	}
	$('#new_request textarea[name="xml"]').val(frag);

	// fill datetime fields
	$('#new_request input[name="scheduled"]').val(isoToLocalDate($(xml).find('scheduled').text()).substring(0,10));
	$('#new_request input[name="reference"]').val(isoToLocalDate($(xml).find('reference').text()));

	// check if xml was repeating
	switchExecutionForm($(xml).find('schedule id').text() != '');
}


