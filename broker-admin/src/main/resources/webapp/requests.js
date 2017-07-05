
function init(){
	$('#new_request').on('submit', function(e){
		e.preventDefault();
		addNewRequest();
	});
	loadRequestList();
	$('#new_request input[name="scheduled"]').val(new Date().toDateInputValue());
	$('#new_request input[name="p_name"]').val('Ihr Name')
	$('#new_request input[name="p_email"]').val('ihre.email@addres.se');
	$('#new_request input[name="title"]').val('Titel der Abfrage')
	$('#new_request input[name="x_ts"]').val(new Date().toDateInputValue()+"T00:00");
}

//$(document).ready(function(){
//	init();
//});
Date.prototype.toDateInputValue = (function(){
	var local = new Date(this);
	local.setMinutes(this.getMinutes() - this.getTimezoneOffset());
	return local.toJSON().slice(0,10);
});
function loadRequestList(){
	$.get({
		url: rest_base+'/broker/request',
		success: function(data) {
			// clear list
			$('#requests').empty();
			// TODO render request list
			xml = $(data);
			xml.find('request').each(function(){
				var id = $(this).attr('id');
				var el = $('<div class="req"><span>request id='+id+'</span> <span class="del">x</span> <span class="show">s</span></div>');
				el.data('id', id);
				$('#requests').append( el );
			});
			$('#requests .del').click(function(){
				var req_el = $(this).parent();
				$.ajax({
					type: 'DELETE',
					url: rest_base+'/broker/request/'+req_el.data('id'),
					success: function(){
						req_el.remove();
						//$('.req[data-id="'+id+'"]').remove();
					}
				})
			});
			$('#requests .show').click(function(){
				var req_el = $(this).parent();
				window.location.href = 'request.html#'+req_el.data('id');
			});
		},
		dataType: "xml"
	});
}
function buildRequestDefinitionXml(requestId, fragment){
	var xml = $.parseXML('<queryRequest xmlns="http://aktin.org/ns/exchange" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><id>'+requestId+'</id><scheduled/><query><id>'+requestId+'</id><title/><description/><principal><name/><organisation/><email/><phone/></principal><schedule xsi:type="singleExecution"><duration/></schedule></query></queryRequest>');
	$(xml).find('scheduled').text($('#new_request input[name="scheduled"]').val());
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

function allocateRequestId(success, error){
			$.post({
				contentType: '',
				url: rest_base+'/broker/request',
				success: function(data, status, xhr) {
					var loc = xhr.getResponseHeader('Location');
					var id = loc.substr(loc.lastIndexOf('/')+1);
					console.log('Request added: '+loc);
					success(loc, id);
				},
				//processData: false,
				//dataType: "xml",
				error: error
			});
}
function setRequestDefinition(location, id){
	// load XML syntax fragment
	// content was already verified to be valid XML in addNewRequest()
	var fragment = $.parseXML($('#new_request textarea[name="xml"]').val());
	var data = buildRequestDefinitionXml(id, fragment);
	$.ajax({ 
		type: 'PUT', 
		data: data,
		contentType: 'application/xml',
		url: location,
		success: function() {
			// definition submitted
			// enable submit button
			$('#new_request button').prop('disabled',false);
			// publish immediately
			$.post(location+'/publish');
			// reload request list
			loadRequestList();
			alert('Request created with id '+id);
			// TODO clear input
			// TODO display status notification of success
		},
		error: function(xhr, s, e){
			$('#new_request button').prop('disabled',false);
			// delete empty request
			$.ajax({ 
				type: 'DELETE', 
				contentType: 'application/xml',
				url: location
			});
			alert('Unable to set request definition for request '+id);
		},
		processData: false,
		dataType: "xml"
	});

}
function addNewRequest(){
	// validate request syntax
	try{
		var fragment = $.parseXML($('#new_request textarea[name="xml"]').val());
	}catch( e ){
		try{
			var ta = $('#new_request textarea[name="xml"]')[0];
			ta.setCustomValidity("No valid XML!")
			ta.reportValidity();
		}catch( e ){
			// HTML5 form validation reporting failed, display alert box
			alert('query definition not valid XML');
		}
		return;
	}

	// disable submit button
	$('#new_request button').prop('disabled',true);
	allocateRequestId(setRequestDefinition, function(){
		$('#new_request button').prop('disabled',false);
		alert('Unable to create new request id');
	});
}

