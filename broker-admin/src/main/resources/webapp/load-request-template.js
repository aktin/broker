
function isoToLocalDate(iso){
	// TODO convert to local timezone
	// remove zone offset from timestamp
	return iso.substring(0,19);
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
			fillForm(fragment);
		},
		error: function(x, m, t){
			alert('Unable to load request with id '+id);
			console.log('Unable to load request',m,t);
		}
		
	});
	fillNodeRestrictionFromTemplate(id);
}


