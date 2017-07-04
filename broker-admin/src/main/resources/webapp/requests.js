
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
function addNewRequest(){
	// validate request syntax
	// disable submit button
	var fragment=null;
	try{
		fragment = $.parseXML($('#new_request textarea[name="xml"]').val());
	}catch( e ){
		alert('query definition not valid XML');
		return;
	}
	var data = buildRequestDefinitionXml(4711, fragment);
	console.log((new XMLSerializer()).serializeToString(data));
//	return;
	// disable submit button until asynchronous call completed
	$('#new_request button').prop('disabled',true);
	$.ajax({ 
		type: 'POST', 
		data: data,
		contentType: 'application/xml',
		url: rest_base+'/validator/check',
		success: function() {
			// syntactically valid, proceed to add the request
			// TODO clear input
			// enable submit button
			$('#new_request button').prop('disabled',false);
			$.post({
				data: data,
				contentType: 'application/vnd.aktin.query.request+xml',
				url: rest_base+'/broker/request',
				success: function(data, status, xhr) {
					var loc = xhr.getResponseHeader('Location');
					console.log('Request added: '+loc);
					// publish immediately
					$.post(loc+'/publish');
					loadRequestList();
				},
				processData: false,
				dataType: "xml"
			});
			// TODO display status notification of success
		},
		error: function(xhr, s, e){
			$('#new_request button').prop('disabled',false);
			alert('invalid');
		},
		processData: false,
		dataType: "xml"
	});
}
