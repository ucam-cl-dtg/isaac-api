
function loadContent(uri, addToHistory) {
	
	
	// Catch URLS that can be rendered without a round-trip to the server
	var renderedLocally = true;
	switch(uri)
	{
	case "/register":
		soy.renderElement($("#content")[0], rutherford.pages.register, null, ij);
		break;
	case "/learn":
		soy.renderElement($("#content")[0], rutherford.pages.learn, null, ij);
		break;
	case "/discussion":
		soy.renderElement($("#content")[0], rutherford.pages.discussion, null, ij);
		break;
	case "/about-us":
		soy.renderElement($("#content")[0], rutherford.pages.about_us, null, ij);
		break;
	case "/real-world":
		soy.renderElement($("#content")[0], rutherford.pages.real_world, null, ij);
		break;
	case "/applying":
		soy.renderElement($("#content")[0], rutherford.pages.applying, null, ij);
		break;
	case "/challenge":
		soy.renderElement($("#content")[0], rutherford.pages.challenge, null, ij);
		break;
	case "/why-physics":
		soy.renderElement($("#content")[0], rutherford.pages.why_physics, null, ij);
		break;
	default:
		renderedLocally = false;
		break;
	}
	
	if (!renderedLocally)
	{
		// We need to request the page from the server. Do that.
		
		var template = null;
		
		if (uri.indexOf("/topics/") == 0)
			template = rutherford.pages.topic;
		
		if (uri.indexOf("/questions/") == 0)
			template = rutherford.pages.question;
		
		if (uri.indexOf("/concepts/") == 0)
			template = rutherford.pages.concept;
		
		
		if (template)
		{
			// This is a URI we know about
			$.get(contextPath + "/api" + uri, function(json) {
				soy.renderElement($("#content")[0], template, json, ij);
				MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
			});
		}
		else
		{
			// Not sure that this URI has a matching template on the server. Die.
			console.error("Template not found for uri", uri);
		}
	}
	
	//var oldLoc = window.location.href;
	//urlHistory.push(oldLoc);
	
	console.log("Leaving", window.location.href);
	console.log("Arriving at", uri);
	if (addToHistory)
	{
		history.pushState(uri,null,uri);	
	}
	
}

//var urlHistory = [document.location.href];

function popHistoryState(e)
{
	console.log("Popping state. Moved to", document.location.href, "State:", e.state);
	//loadContent(document.location.href);
	if (e.state !== null)
	{
		if (e.state == "<HOME>") //ARRRRGH. This is horrible. Don't do this.
		{
			document.location.reload();
		}
		else
		{
			loadContent(e.state, false);
		}
	}
}


function click_a(e)
{
	var uri = $(e.target).data("contentUri");
	
	if (uri != undefined)
	{
		console.log("Loading URI", uri);
		
		loadContent(uri, true);
		
		// Hack to close dropdowns:
		$(".hover").removeClass("hover");		
		
		return false;
	}
}

function mouseenter_a(e)
{
	var physicsLinks = $(e.target).data("physicsLinks") || "";
	var mathsLinks = $(e.target).data("mathsLinks") || "";
	var questionLinks = $(e.target).data("questionLinks") || "";
	
	var links = [];
	links = links.concat(physicsLinks !== "" ? physicsLinks.split(",") : []);
	links = links.concat(mathsLinks !== "" ? mathsLinks.split(",") : []);
	links = links.concat(questionLinks !== "" ? questionLinks.split(",") : []);
	
	$("a").removeClass("related-link");
		
	if (links.length > 0)
	{
		
		$(e.target).addClass("related-link");
		for(var i in links)
		{
			$('#' + links[i]).addClass("related-link");
		}
	}
}

function checkAnswer_click(e)
{
	var correct = true;
	$("input[type='checkbox']").each(function(i,e)
	{
		correct = correct && (e.value == "1" && e.checked  || e.value == "0" && !e.checked);
	});
	$("input[type='radio']").each(function(i,e)
	{
		correct = correct && (e.value == "1" && e.checked  || e.value == "0" && !e.checked);
	});
	$("input[type='text']").each(function(i,e)
	{
		correct = correct && ($(e).data("expectedAnswer") == e.value);
	});
	
	if(correct)
	{
		$(".question-wrong").hide();
		$(".question-explanation").fadeIn(200);
		$("#checkAnswer").hide();
	}
	else
	{
		$(".question-explanation").hide();
		$(".question-wrong").fadeIn(200);
	}
	console.log(correct)
}

function button_click(e)
{
	if ($(e.target).data("playVideo"))
	{
		playVideo($(e.target).data("playVideo"));
	}
}

function playVideo(video)
{
	$("#video-modal video").remove();
	$("#video-modal").append($('<video width="640" height="480" controls autoplay/>').attr("src", contextPath + "/static/video/" + video));
	$('#video-modal').foundation('reveal', 'open');
}


$(function()
{
	$("body").on("click", "a", click_a);
	$("body").on("mouseenter", "a", mouseenter_a);
	$("body").on("click", "#checkAnswer", checkAnswer_click);
	$("body").on("click", "button", button_click);
	
	$("#video-modal").on("closed", function()
	{
		$("video").remove();
	});
	
	window.addEventListener("popstate", popHistoryState);

	history.replaceState("<HOME>", null, ij.contextPath + "/soy/rutherford.main"); // Ugh.
	
	MathJax.Hub.Config({
		  tex2jax: {inlineMath: [['$','$'], ['\\(','\\)']]}
		});
});