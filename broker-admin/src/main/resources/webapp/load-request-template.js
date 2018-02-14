
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
}

function fillNodeRestrictionFromTemplate(id){

	$.get({
		url: rest_base+'/broker/request/'+id+'/nodes',
		dataType: 'xml',
		success: function(data){
			// check radio
			$("#limit_target_s").prop("checked", true);
			//$("#limit_target_a").prop("checked", false);
			$("#target").prop("disabled", false);			
			// clear list selections first
			$("#target").val([]);
			// setting select.val to array somehow doesn't work on chrome
			var nodes = $(data).find('node').map(function(){return $(this).text();}).get();
			// XXX find out why a delay is needed. without delay, the selection is cleared shortly after
			setTimeout(function(){
				console.log('Setting node restrictions to',nodes);
				$("#target").val(nodes);
			}, 600);
		},
		error: function(x,m,t){
			//$("#limit_target_s").prop("checked", false);
			$("#limit_target_a").prop("checked", true);
			$("#target").prop("disabled", true);
			// clear list selections
			$("#target").val([]);
			if( x.status == 404 ){
				// template not restricted to certain nodes
			}else{
				console.log('Unable to request node restriction: '+m, t);
			}
		}
	});
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
	fillNodeRestrictionFromTemplate(id);
}


