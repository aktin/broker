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
			dataType: "xml"
		});
}
function getNodes(success, error){
	$.ajax({ 
		type: 'GET', 
		url: rest_base+'/broker/node',
		success: function(data) {
			var a = [];
			var nodes = data.getElementsByTagNameNS('http://aktin.org/ns/exchange','node');
			for( var i=0; i<nodes.length; i++ ){
				var id=nodes[i].firstChild.innerHTML;
				a[id] = {
					id: id,
					dn: nodes[i].childNodes[1].innerHTML,
					seen: nodes[i].childNodes[2].innerHTML
				};
			}
			success(a);
		},
		error: error,
		dataType: "xml"
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
		updateAuthStatus(init);
	}else{
		// no token cookie, redirect to login
		window.location.replace('login.html');
	}
});