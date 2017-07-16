
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
	// load nodes
	getNodes(function(nodes){
		for( var i=0; i<nodes.length; i++ ){
			$('#target')
	         .append($("<option></option>")
	                    .attr("value",nodes[i].id)
	                    .text(nodes[i].dn)); 
		}
		console.log('Loaded nodes: '+nodes.length);
	},function(){
		alert('DWH-Knoten konnten nicht geladen werden.');
	});
	$('#limit_target_a').click(function(){
		// clear selection
		$("#target option:selected").prop("selected", false);
		$("#target").prop("disabled", true);
	});
	$('#limit_target_a').prop("checked", true).trigger("click");
	$('#limit_target_s').click(function(){
		// clear selection
		$("#target").prop("disabled", false);
	});
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
				var cls = 'req';
				if( $(this).find('published').text() != '' ){
					cls += ' published';
				}
				if( $(this).find('closed').text() != '' ){
					cls += ' closed';
				}
				var el = $('<div class="'+cls+'"><span>request id='+id+'</span> <span class="del">x</span> <span class="show">s</span></div>');
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
function localTimeToISO(local_str){
	if( local_str.length == 10 ){
		local_str += "T00:00";
	}
	if( local_str.length == 16 ){
		local_str += ":00";
	}
	
	// TODO parse local_str and convert and return YYYY-MM-DDT00:00:00Z
	local_str += "Z"; // XXX 
	return local_str;
}
function buildRequestDefinitionXml(requestId, fragment){
	var xml = $.parseXML('<queryRequest xmlns="http://aktin.org/ns/exchange" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><id>'+requestId+'</id><reference/><scheduled/><query><title/><description/><principal><name/><organisation/><email/><phone/></principal><schedule xsi:type="singleExecution"><duration/></schedule></query></queryRequest>');
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

function allocateRequestId(success, error){
			$.post({
				contentType: '',
				url: rest_base+'/broker/request',
				success: function(data, status, xhr) {
					var loc = xhr.getResponseHeader('Location');
					var id = loc.substr(loc.lastIndexOf('/')+1);
					console.log('Request added: '+loc);
					setRequestDefinition(loc, id, success, function(message){
						// all failures from this point on should
						// delete the request
						console.log('Deleting request '+id+'...');
						$.ajax({ 
							type: 'DELETE', 
							contentType: 'application/xml',
							url: loc
						});
						error(message);
					});
				},
				//processData: false,
				//dataType: "xml",
				error: function(x, m, t){
					error('Unable to allocate new request ID: '+m);
				}
			});
}
function buildNodesXml(nodeIds){
	var xml = $.parseXML('<nodes xmlns="http://aktin.org/ns/exchange"></nodes>');
	for( var i=0; i<nodeIds.length; i++ ){
		$(xml).find('nodes').append('<node>'+nodeIds[i]+'</node>');
	}
	return xml;
}

function setRequestDefinition(location, id, success_fn, error_fn){
	// load XML syntax fragment
	// content was already verified to be valid XML in addNewRequest()
	var fragment = $.parseXML($('#new_request textarea[name="xml"]').val());
	var data = buildRequestDefinitionXml(id, fragment);
	$.ajax({ 
		type: 'PUT', 
		data: data,
		processData: false,
		contentType: 'application/xml',
		url: location,
		success: function() {
			// definition submitted
			if( $("#limit_target_s").prop("checked") ){
				// limit request to certain nodes
				// build <nodes><node>1</node>... and post to location+'/nodes
				var n = $('#target').val();
				var x = buildNodesXml(n);
				console.log('Limiting request target nodes to '+n);
				$.ajax({
					type: 'PUT', 
					data: x,
					processData: false,
					contentType: 'application/xml',
					url: location+'/nodes',
					success: function(){
						console.log('..ok');
						success_fn(location,id);
					},
					error: function(x,m,t){
						console.log('..failed');
						error_fn('Failed to limit destination nodes: '+m);
					}
				});
			}else{
				console.log('Request not limited to certain target nodes');
				success_fn(location, id);
			}
		},
		error: function(x, m, t){
			error_fn('Unable to set request definition for request '+id+': '+m);
		},
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
	allocateRequestId(function(location, id){
		// success
		$('#new_request button').prop('disabled',false);
		// publish immediately
		console.log('Publishing request '+location);
		$.post(location+'/publish');	
		// reload request list
		loadRequestList();
		alert('Request created with id '+id);
		// TODO clear input
		// TODO display status notification of success
	}, 
	function(){
		// failed
		$('#new_request button').prop('disabled',false);
		alert('Unable to create new request id');
	});
}

