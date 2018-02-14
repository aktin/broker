
function isoToLocalDate(iso){
	// TODO convert to local timezone
	// remove zone offset from timestamp
	return iso.substring(0,19);
}

function fillFormFromRequestXml(xml){

	// fill form elements with xml info
	$('#new_request *').filter(':input').each(function(){
		var jpath = $(this).data('jpath');
		if( jpath == '' )return;
		$(this).val( $(xml).find(jpath).text() );
	});

	// compile query contents
	var query = $(xml).find('query')[0];
	console.log('Query element:', query);
	var frag = '';
	var xs = new XMLSerializer();
	for( var i=0; i<query.childNodes.length; i++ ){
		frag += xs.serializeToString(query.childNodes[i]);
	}
	
	$('#new_request textarea[name="xml"]').val(frag);

	// fill datetime fields
	$('#new_request input[name="scheduled"]').val(isoToLocalDate($(xml).find('scheduled').text()).substring(0,10));
	$('#new_request input[name="reference"]').val(isoToLocalDate($(xml).find('reference').text()));
	
}

// fill form from existing request template
function fillFormFromTemplate(id){
	// load request definition
	$.get({
		url: rest_base+'/broker/request/'+id,
		dataType: 'text',
		success: function(data){
			var fragment = $.parseXML(data);
			console.log('Request definition retrieved for '+id, fragment);
			fillFormFromRequestXml(fragment);
		},
		error: function(x, m, t){
			alert('Unable to load request with id '+id);
			console.log('Unable to load request',m,t);
		}
		
	});
}


