var rest_base='../..';
function updateAuthStatus(onSuccess){
		$.ajax({ 
			type: 'GET', 
			url: rest_base+'/auth/status',
			success: function(data) {
				// TODO remember token expiration date
				onSuccess();
			},
			error: function(xhr, s, e){
				console.log('Session abgelaufen: '+s);
				window.location.replace('login.html');
			},
			dataType: "json"
		});
}
function initializePage(){
	$('#new_request').on('submit', function(e){
		e.preventDefault();
		addNewRequest();
	});
	loadRequestList();	
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
				var el = $('<div class="req"><span>request id='+id+'</span><span class="del">x</span></div>');
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
		},
		dataType: "xml"
	});
}
function addNewRequest(){
	// validate request syntax
	// disable submit button
	$('#new_request button').prop('disabled',true);
	var data = $('#new_request textarea').val();
	$.ajax({ 
		type: 'POST', 
		data: data,
		contentType: 'application/xml',
		url: rest_base+'/validator/check',
		success: function(data) {
			// syntactically valid, proceed to add the request
			// clear input
			$('#new_request textarea').val('');
			// enable submit button
			$('#new_request button').prop('disabled',false);
			$.post({
				data: data,
				contentType: 'application/vnd.aktin.request+xml',
				url: rest_base+'/broker/request',
				success: function(data, status, xhr) {
					var loc = xhr.getResponseHeader('Location');
					console.log('Request added: '+loc);
					// publish immediately
					$.post(loc+'/publish');
					loadRequestList();
				}
			});
			// TODO display status notification of success
		},
		error: function(xhr, s, e){
			$('#new_request button').prop('disabled',false);
			alert('invalid');
		},
		dataType: "text"
	});
}
$(document).ready(function(){
	// check if cookie is set
	var token = Cookies.get('token');
	if( token ){
		$.ajaxSetup({beforeSend: function (xhr){
			xhr.setRequestHeader("Authorization","Bearer "+token);        
		}});
		// cookie still valid?
		updateAuthStatus(initializePage);
	}else{
		// no token cookie, redirect to login
		window.location.replace('login.html');
	}
});