
function init(){
	$('#new_request').on('submit', function(e){
		e.preventDefault();
		addNewRequest();
	});
	$('#submit_request').on('submit', function(e){
		e.preventDefault();
		addNewRequest();
	});
	// load form script first, then the form html, initialize form afterwards
	loadTemplateTypes();
	loadRequestList();
	// load nodes
	getNodes(function(nodes){
		for( var i=0; i<nodes.length; i++ ){
			$('#target').append($("<option/>",{
	        	value: nodes[i].id,
	        	text: nodes[i].cn,
	        	title: nodes[i].dn
	        }));
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
		$("#target").prop("disabled", false);
	});
	// hide new request form
	$('#choose_template button').click(function(e){
		e.preventDefault();
		var tt = $('#template_type').val();
		$(this).parent().remove();
		$.getScript('template/'+tt+'/script.js', function(){
			$('#new_request').load('template/'+tt+'/form.html', function(){
				initializeForm();
				$('#create').show();
			});
		});

	});
	$('#create').hide();
	
}

//$(document).ready(function(){
//	init();
//});
Date.prototype.toDateInputValue = (function(){
	var local = new Date(this);
	local.setMinutes(this.getMinutes() - this.getTimezoneOffset());
	return local.toJSON().slice(0,10);
});

function loadTemplateTypes(){
	$.getJSON(rest_base+'/template', function(data){
		console.log("Template",data);
		for( key in data ){
				$('#template_type').append($('<option>', {
    				value: key,
    				text: data[key].title
				}));		
		}
	});
}

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
				var el = $('<div class="'+cls+'"><span>request id='+id+'</span> <span class="del">x</span> <span class="export"></span> <span class="show">s</span></div>');
				el.data('id', id);
				var req_type = $(this).find('type').text();
				el.data('type', req_type);
				console.log('Request type for',id,req_type);
				$('#requests').append( el );
			});
			$('#requests .del').click(function(){
				var req_el = $(this).parent();
				if( confirm('Please confirm to delete request id '+req_el.data('id')) ){				
					$.ajax({
						type: 'DELETE',
						url: rest_base+'/broker/request/'+req_el.data('id'),
						success: function(){
							req_el.remove();
							//$('.req[data-id="'+id+'"]').remove();
						}
					});
				}
			});
			$('#requests .show').click(function(){
				var req_el = $(this).parent();
				window.location.href = 'request.html#'+req_el.data('id');
			});
			$('#requests .export').each(function(){
				var req_el = $(this).parent();
				var req_id = req_el.data('id');
				$(this).append('<a class="indirectDownload" data-url="'+rest_base+'/broker/export/request-bundle/'+req_id+'" href="about:blank">e</a>');
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
		var el = xml.createElementNS('http://aktin.org/ns/exchange','node');
		el.innerHTML = nodeIds[i];
		xml.documentElement.appendChild(el);
	}
	return xml;
}

function setRequestDefinition(location, id, success_fn, error_fn){
	// load XML syntax fragment
	// content was already verified to be valid XML in addNewRequest()
	var data = compileForm(id);
	$.ajax({ 
		type: 'PUT', 
		data: data,
		processData: false,
		contentType: getFormMediaType(), // use aktin mime type
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

	if( validateForm() == false ){
		return;
	}
	// disable submit button
	// TODO XXX make sure the form can not be submitted by pressing enter in a field
	
	$('#submit_request button').prop('disabled',true);
	allocateRequestId(function(location, id){
		// success
		$('#submit_request button').prop('disabled',false);
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
