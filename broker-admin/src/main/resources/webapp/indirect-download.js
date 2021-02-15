
// to create indirect downloads
// add elements like <a class="indirectDownload" data-url="<url which returns the final url as content>" href="about:blank">some text</a>
// these links will be resolved when clicked. Future clicks will directly proceed
// to the resolved url.

function resolveIndirectDownload(url, success){
	$.post({
		url: url,
		dataType: 'text',
		success: function(data){
			console.log('Resolved download to '+data);
			var new_url = rest_base+'/broker/download/'+data;
			success( new_url );
		},
		error: function(x,m,t){
			if( x.status == 404 ){
				alert('No data available');
				// template not restricted to certain nodes
			}else{
				console.log('Unable to resolve download: '+m, t);
			}
		}
	});
}

$(document).on("click","a.indirectDownload", function () {
	// reference to clicked a element
	var a = $(this);
   	var url = a.data('url');
   	console.log('Clicked on indirect download '+ url);
   	// resolve download
   	resolveIndirectDownload(url, function(new_url){
		// retrieved download id
		// replace the href for subsequent clicks
		a.attr('href', new_url);
		// remove class, so this method is not called again
		a.removeClass('indirectDownload');
		// redirect to the resolved download url
		setTimeout(function(){
			window.location.href = new_url;
		}, 500); // redirect after 500 milliseconds
   	});
	return false;
});
