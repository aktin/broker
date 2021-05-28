
function isoToLocalDate(iso){
	console.log("ISO String",iso);
	newTime=new Date(iso);
	//get offset
	offset = newTime.getTimezoneOffset();

	// apply date specific timezone
	newDate = new Date(newTime.getTime() - (offset*60*1000));
	//cut off timezone information after applying offset
	finalResult=newDate.toISOString().split('Z')[0];
	console.log("localized String to German timezone",finalResult);
	return finalResult;
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

function isContentTypeCompatible(contentType, typeSpec){
	var p = contentType.split(';');
	if( p.length > 0 ){
		contentType = p[0];
	}
	// TODO compare media type compatibility instead of equality
	return( contentType == typeSpec );	
}

// fill form from existing request template
function fillFormFromTemplate(id){
	// load request definition
	$.get({
		url: rest_base+'/broker/request/'+id,
		dataType: 'text',
		success: function(data, status, xhr){
			console.log('Request definition retrieved for '+id, data);
			var contentType = xhr.getResponseHeader('content-type');
			// remove charset info
			
			if( isContentTypeCompatible(contentType,getFormMediaType()) ){
				fillForm(data, contentType, id);
				fillNodeRestrictionFromTemplate(id);
			}else{
				alert('Form not compatible with request '+id);
				console.log('Request type',contentType,'Form type',getFormMediaType());
			}
		},
		error: function(x, m, t){
			alert('Unable to load request with id '+id);
			console.log('Unable to load request',m,t);
		}
		
	});
}


