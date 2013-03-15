var Dom=YAHOO.util.Dom;
var C_WHITE="#ffffff";
var C_BLACK="#000000";
var C_ROYALBLUE="#3875d7";
var C_LIGHTBLUE="#dae2ed";
function initLogin(event){
	Dom.get('login').style.visibility = 'visible';
	Dom.get('login').style.display = 'block';
	Dom.get('username').focus();
	new Rico.Effect.FadeTo('login', 1, 1000, 10, {
		complete:function() {}
	});
	Dom.get('jsFrame').src = "js_preload.html";
}
function isCursorOnInputArea(event){
	var e=event||window.event;
	if(!e) return true;
	var src=e.srcElement||e.target;
	var t="";
	if(src) t=src.tagName.toLowerCase();
	if(t=='textarea'||t=='iframe'||t=='input') return true;
	else return false;
}
function selectstart(event){
	return isCursorOnInputArea(event);
}
function loadChat() {
	preloadChatImages();
	initChatLogin();
	statusTimerIncrement();
	statusListener();
	initToolbar();
	if (window.addEventListener) {
		window.addEventListener("click", outInfoWin, false);
		window.addEventListener("mousemove", arrangeStatus, false);
	} else if (window.attachEvent && !window.opera){
		window.attachEvent("onclick",outInfoWin);
		window.attachEvent("onmousemove", arrangeStatus);
	}
}
function unloadHandler() {
	var url = "profiling/logout.cl";
	var callback = {
	  success: 	function(o) {},
	  failure: 	function(o) {},
	  argument: []
	}
	var request = YAHOO.util.Connect.asyncRequest('GET', url, callback);
}
function onloadHandler(event){
	try{onloadH(event)}catch(e){}
	try{loginProgress()}catch(e){}
	try{loadPreferences()}catch(e){}
	checkingMail=false;
	try{checkMail(null,null,true)}catch(e){}
	try{fetchFolders()}catch(e){}
	try{checkUnreadMailCountFirstTime()}catch(e){}
	try{loadChat()}catch(e){}
	try{window.setTimeout(getContacts,3000)}catch(e){}
	try{clearContactForm()}catch(e){}
	try{window.setTimeout(getNotebooks,6000)}catch(e){}
	try{window.setTimeout(getRssNews,4000)}catch(e){}
	try{preloadCommonImages()}catch(e){}
	try{window.setTimeout(notesCleaner,300000)}catch(e){}
}
function initPage() {
	fixLayout();
	var tools = document.getElementById("tools");
	if (tools && (window.attachEvent && !window.opera)){
		var nodes = tools.getElementsByTagName("li");
		for (var i=0; i<nodes.length; i++)
		{
			nodes[i].onmouseover = function() {
				if ((this.className.indexOf("sub")) != -1)
					this.className += " hover";
			}
			nodes[i].onmouseout = function() {
				this.className = this.className.replace(new RegExp(" hover"),"");
			}
		}
	}

	var links = document.getElementsByTagName("a");
	var mailList = document.getElementById("mailList");
	if (links && mailList){
		for (var i=0; i<links.length; i++)
		if (links[i].className.indexOf("toggle") != -1){
			links[i].onclick = function() {
				if ((this.className.indexOf("on") != -1) && (mailList.parentNode.className.indexOf("full") != -1)){
					mailList.parentNode.className = mailList.parentNode.className.replace(new RegExp(" full"),"");
					mailList.style.display = "";
					}
				if ((this.className.indexOf("off") != -1) && !(mailList.parentNode.className.indexOf("full") != -1))
					mailList.parentNode.className += " full";
				fixLayout();
			}
		}
	}
	Dom.get('loginstatus').style.display = 'none';
	renderDivs();
	try {
		registerAutoComplete("to", "autoCompleteTo");
		registerAutoComplete("cc", "autoCompleteCc");
		registerAutoComplete("bcc", "autoCompleteBcc");
		
		//IE BUG&FIX
		//Prevents javascript error which 'contentWindow.document.body' is null 
		//or not an object when compose is loading first.
		if(tinyMCE.getEditorId('composeBody')==null)
			tinyMCE.execCommand('mceAddControl',true,'composeBody');

	} catch (e) {}
}

function initPng() {
	if (navigator.appVersion.indexOf("MSIE 6") != -1){
		var images = document.getElementsByTagName("img");
		if (images){

			for (var i = 0; i < images.length; i++){
				if ((images[i].src.indexOf(".png")) != -1){
					var srcname = images[i].src.replace(new RegExp('(.*)\/(.*)?\.png'),"$2");
					images[i].parentNode.style.display = "inline-block"
					images[i].style.visibility = "hidden";
					images[i].style.marginTop = "0";
					images[i].parentNode.style.filter = "progid:dximagetransform.microsoft.alphaimageloader(src='images/"+ srcname +".png',sizingmethod='crop');"
				}
			}
		}
	}
}

var windowheight;
var windowwidth;
var mainHeight;
var minHeight = 490;
var wysiwygHeight = 300;

function fixLayout() {
	var main = document.getElementById("main");
	var mailList = document.getElementById("mailList");
	var mailBody = document.getElementById("mailBody");
	var newMessages = document.getElementById("newMessages");
	var events = document.getElementById("events");
	var news = document.getElementById("news");
	var folders = document.getElementById("folders");
	var leftColumn = document.getElementById("leftColumn");
	var rightColumn = document.getElementById("rightColumn");
	var calendar = document.getElementById("calendar");
	var calendars = document.getElementById("calendars");
	var tasks = document.getElementById("tasks");
	var chat = document.getElementById("chat");
	var contactFolders = document.getElementById("contactFolders");
	var contactList = document.getElementById("contactListReal");
	var noteFolders = document.getElementById("notesFolders");
	var noteList = document.getElementById("noteFolderListReal");
	var notesDetails = document.getElementById("notesDetails");
	var memoBoard = document.getElementById("memoBoard");

	if (main) {
		main.style.height = "0";
		mainHeight = document.documentElement.clientHeight - 82;
		if (window.opera) mainHeight = document.body.clientHeight - 82;
		if (navigator.appVersion.indexOf("Safari") != -1) mainHeight = self.innerHeight - 82;

		windowwidth = document.documentElement.clientWidth;
		if (window.opera) windowwidth = document.body.clientWidth;
		if (navigator.appVersion.indexOf("Safari") != -1) windowwidth = self.innerWidth;

		if (mainHeight < minHeight) mainHeight = minHeight;
		if (loggedIn) {
			main.style.height = mainHeight + "px";
		}
		windowheight = mainHeight + 82;
	}

	document.body.style.overflow = "hidden";
	
	// WYSIWYG Width and Height
	
	var attr = Dom.get('attachmentstr');
	var diff = 0;
	if (attr) {
		diff = attr.offsetHeight;
	}
	wysiwygHeight = windowheight - (295 + diff);
	if (wysiwygHeight < 200) {
		wysiwygHeight = 200;
	}
	try {
		if (!document.all) {
			var eid = tinyMCE.getEditorId('composeBody');
			Dom.get('composeBody').style.height = wysiwygHeight + "px";
			var te = Dom.get(eid);
			te.style.height = (wysiwygHeight - 29) + "px";
			te.parentNode.parentNode.parentNode.style.height= (wysiwygHeight - 5) + "px";
			te.parentNode.parentNode.parentNode.parentNode.style.height= (wysiwygHeight -5) + "px";
		}
	} catch (e) {}

	// locate the folder actions buttons
	try {
		if (!folderActionsExpanded) {
			Dom.get('folderActionsBtn').style.top = mainHeight + "px";
			Dom.get('folderActions').style.display = 'none';
		} else {
			Dom.get('folderActionsBtn').style.top = (getHeight(Dom.get('folders')) - 143) + "px";
			Dom.get('folderActions').style.top = (getHeight(Dom.get('folders')) - 143) + "px";
			Dom.get('folderActions').style.display = 'block';
		}
	} catch (e) {}

	// arrange mail, contact folders etc...		
	if (folders) folders.style.height = mainHeight + "px";
	if (contactFolders) contactFolders.style.height = mainHeight + "px";
	if (contactList) contactList.style.height = (mainHeight - 80) + "px";
	if (noteFolders) noteFolders.style.height = mainHeight + "px";
	if (noteList) noteList.style.height = (mainHeight - 50) + "px";
	if (notesDetails) notesDetails.style.height = mainHeight + "px";
	if (memoBoard) memoBoard.style.height = (mainHeight - 120) + "px";

	try {
		var mtc = Dom.get('mailtitleCloneDiv');
		if (mtc) {
			var lst1 = Dom.get('mailList');
			if (lst1) {
				if (lst1.offsetWidth && lst1.offsetWidth > 0) {
					var psCount = lst1.getElementsByTagName('p').length;
					
					var lstHeight = lst1.style.height;
					if (lstHeight.indexOf('px') > 0) {
						lstHeight = lstHeight.substr(0, lstHeight.length - 2);
					}
					var iLastHeight = parseInt(lstHeight);
					maxP = 7;
					try {
						if (iLastHeight != null && iLastHeight > 0) {
							maxP = parseInt(iLastHeight / 18);
						}
					} catch (jkl) {}
					if (psCount > maxP) {
						mtc.style.width = (lst1.offsetWidth - 18) + "px";
					} else {
						mtc.style.width = (lst1.offsetWidth - 2) + "px";
					}
					var xy = getAbsolutePosition(lst1);
					mtc.style.top = (xy['y'] + 1 ) + "px";
					mtc.style.left = (xy['x'] + 1) + "px";
				}
			}
		}
	} catch (p) {}

	part = parseInt((mainHeight - 83) / 3);

	if (chat) {
		chat.style.height = mainHeight + "px";
		if (logged) {
			initToolbar();
		}
		initContacts();
	}

	if (mailList) mailList.style.height = part - 2 +  "px";

	if (mailBody) mailBody.style.height = part * 2 + 10 + "px";
	if (mailList && mailBody && (mailBody.parentNode.className.indexOf("full")) != -1) {
		mailList.style.display = "none";
		mailBody.style.height = part * 3 + 31 + "px";
	}
	fixMsgViewIframeLayout();
	
	if (events && news){
		var inners = main.getElementsByTagName("div");
		for (var i = 0; i < inners.length; i++){
			if (inners[i].className.indexOf("inner") != -1)
			{
				inners[i].style.height = "0";
//				inners[i].style.width = "0";
			}
		}
		itsheight = (mainHeight - 84 - 60);
		newMessages.style.height = itsheight + "px";
		itshalf = parseInt(itsheight / 2);
		events.style.height = itshalf + "px";
		news.style.height = itshalf + "px";
		if (itsheight%2 != 0) news.style.height = itshalf + 1 + "px";
		var inners = main.getElementsByTagName("div");
		for (var i = 0; i < inners.length; i++){
			if (inners[i].className.indexOf("inner") != -1) inners[i].style.height = inners[i].parentNode.parentNode.parentNode.parentNode.parentNode.style.height;
			if (inners[i].className.indexOf("inner") != -1) inners[i].style.width = inners[i].parentNode.parentNode.parentNode.parentNode.parentNode.offsetWidth - 56 + "px";
		}
	}

	if (leftColumn) {
		leftColumn.style.height = mainHeight + "px";
		rightColumn.style.height = mainHeight + "px";
	}
	if (leftColumn && rightColumn){
		calendars.style.height = mainHeight - 205 + "px";
		tasks.style.height = mainHeight - 20 + "px";
		var calendardiv = calendar.getElementsByTagName("div");

		for (var i = 0; i < calendardiv.length; i++){
			if (calendardiv[i].className.indexOf("event") != -1){
				try {
					calendardiv[i].style.width = calendardiv[i].parentNode.offsetWidth - 57 + "px";
					var eventp = calendardiv[i].getElementsByTagName("p");
					for (var j = 0; j < eventp.length; j++) {
						if (eventp[j].className.indexOf("title") != -1) {
							var duration = eventp[j].lastChild.nodeValue;
							while (duration.indexOf(":") != -1) {
								duration = duration.replace(":",".");
								duration = duration.replace(":30",".5");
							}
							var a = duration.split("-");
							duration = Number(a[0]) - Number(a[1]);
							one = calendardiv[i].parentNode.offsetHeight;
							calendardiv[i].style.height = one * (duration * -1) - 17 + "px";
							if (Number(a[0])/parseInt(Number(a[0])) != 1) {
								calendardiv[i].style.marginTop = parseInt(one / 2) - 4 + "px";
								calendardiv[i].style.height = one * (duration * -1) - 20 + "px";
							}
						}
					}
				} catch (e) {
				//	alert(e.message);
				}
			}
		}
	}
}

if (window.addEventListener) {
	window.addEventListener("load",initPage,false);
	window.addEventListener("resize",fixLayout,false);
} else if (window.attachEvent && !window.opera){
	window.attachEvent("onload",initPage);
	window.attachEvent("onresize",fixLayout);
	window.attachEvent("onload",initPng);
}

function trim(str) {
	if (str == null) return null;
	return str.replace(/^\s*|\s*$/g,"");
}

function htmlSpecialChars(str) {
	str = str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\"/g, "&quot;");
	return str;
}

function undoHtmlSpecialChars(str) {
	str = str.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, "\"").replace(/&amp;/g, "&");
	return str;
}

function nl2br(str) {
	str = str.replace(/\n/g, "<br />");
	return str;
}

function br2nl(str) {
	str = str.replace(/<br \/>/g, "\n");
	str = str.replace(/<br>/g, "\n");
	str = str.replace(/<br\/>/g, "\n");
	return str;
}
function getFirstValByTagName(o, tn){
	var n = o.getElementsByTagName(tn);
	var fn = n[0].firstChild;
	if(null != fn) return fn.nodeValue;
	else return n.item(0).text;
}
function getFirstCleanValByTagName(o, tn){
	return trim(undoHtmlSpecialChars(getFirstValByTagName(o, tn)));
}

function getAbsolutePosition(element){
    var ret = new Point();
    for(;
        element && element != document.body;
        ret.translate(element.offsetLeft, element.offsetTop), element = element.offsetParent
	);
    return ret;
}

function Point(x,y){
	this.x = x || 0;
	this.y = y || 0;
	this.toString = function(){
	    return '('+this.x+', '+this.y+')';
	};
	this.translate = function(dx, dy){
	    this.x += dx || 0;
	    this.y += dy || 0;
	};
	this.getX = function(){ return this.x; }
	this.getY = function(){ return this.y; }
	this.equals = function(anotherpoint){
	    return anotherpoint.x == this.x && anotherpoint.y == this.y;
	};
}

function safeEncode(str) {
	if (str == null) {
		return "";
	}
	str = encodeURI(str);
	str = str.replace(new RegExp("&",'g'), "%26");
	str = str.replace(new RegExp(";",'g'), "%3B");
	str = str.replace(new RegExp("[?]",'g'), "%3F");
	return str;
}
 