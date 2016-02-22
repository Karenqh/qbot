/**
 * 
 */

var chat_box = document.getElementById("chat-box");
var chat_msg = document.getElementById("chat-msg");
var pub_key = "pub-c-d77705ee-8dc9-417b-9c49-c726f65d4eeb";
var sub_key = "sub-c-db247f12-d42b-11e5-8a35-0619f8945a4f";
var standby_suffix = "-stdby";
var robotStdbyCh = ""; //robot will publish to this channel
var userStdbyCh = "";  //robot will listen to this channel for control messages
var userId = "";
var robotId = "";

//Initialize PubNub
function login() {
	var inputbox = document.getElementById("username")
	userId = safeId(inputbox.value) || "Anonymous";
	userStdbyCh = userId + standby_suffix;
	var pubnub = window.pubnub = PUBNUB({
		publish_key : pub_key,
		subscribe_key : sub_key,
		uuid : userId
	});
    //check connectivity
	pubnub.subscribe({
		channel : userStdbyCh,	
		message : userStdbyChMessageCB,
		connect : function(e) {
			var button = document.getElementById("login_submit");
			button.disabled = true;
			button.className = "btn btn-success disabled";
			console.log("Connected to PubNub and ready!");
		}
	});
	return false;
}

function connectRobot() {
	if (!window.pubnub)
		alert("Login First!");
	var input = document.getElementById("robotname");
	robotId = safeId(input.value);
	robotStdbyCh = robotId + standby_suffix;
	// Check robot status and wake up robot
	window.pubnub.state({
	    channel:robotStdbyCh,
	    uuid : robotId,
	    callback: function(m){
	    	if (m.status=="Available"){
	    		buttonConnected();
	    	}
	    	else {
	    		var msg = {
	    				"pn_gcm" : {"data" : {message : "GCMwakeUpCall_"+robotId}}
	    			};
	    		sendRobotMessage("GCMPush", msg)
	    		console.log("Publish to gcm channel: " + robotId);
	    	}
	    }
	});
	return false;
}

function phoneStart() {
	var video_in = document.getElementById("vidIn");
	// var video_out = document.getElementById("vidOut");
	var phone = window.phone = PHONE({
		number : userId || "Anonymous", // listen on username line else anonymous
		publish_key : pub_key, // Your Pub Key
		subscribe_key : sub_key, // Your Sub Key
		oneway : true, // one way stream
	});
	phone.ready(function(){
		console.log("Phone ON!");
	});
	phone.receive(function(session){
	    session.connected(function(session) {
			document.getElementById("vidBlock").style.display = 'flex';
			document.getElementById("motionBlock").style.display = 'flex';
	    	video_in.innerHTML="";
	    	video_in.appendChild(session.video);  
	    });
	    session.ended(function(session) { 
	    	video_in.innerHTML=""; 
	    	document.getElementById("vidBlock").style.display = 'none';
	    	document.getElementById("motionBlock").style.display = 'none';
	    });
	});
	return false;
}

function makeCall() {
	var msg = {
		"call_user" : userId,
		"call_time" : new Date().getMilliseconds()
	};
	console.log("Calling robot...");
	sendRobotMessage(robotStdbyCh, msg);
	if (!window.phone)
		phoneStart();
	return false;
}

function audioToggle(){
	var audio = ctrl.toggleAudio();
	if (!audio) $("#audioToggle").html("Audio Off");
	else $("#audioToggle").html("Audio On");
}

function videoToggle(){
	var video = ctrl.toggleVideo();
	if (!video) $('#videoToggle').html('Video Off'); 
	else $('#videoToggle').html('Video On'); 
}

function endCall() {
	if(window.phone) window.phone.hangup();
}

function disconnectRobot() {
	// tell robot to exit app
	window.pubnub.state({
	    channel:robotStdbyCh,
	    uuid : robotId,
	    callback: function(m){
	    	if (m.status=="Available"){
	    		var msg = {
	    				"power" : "power_off",
	    				"call_time" : new Date().getMilliseconds()
	    			};
	    		sendRobotMessage(robotStdbyCh, msg)
	    		console.log("Turning off robot");
	    	}
	    	else {
	    		buttonDisconnected();
	    	}
	    }
	});
	return false;
}

function sendRobotMessage(ch, msg) {
	if (!window.pubnub)
		alert("Login First!");
	window.pubnub.publish({
		channel : ch,
		message : msg,
		callback : function(m) {
			console.log('send message to robot.');
		}
	});
}

//process messaged received from robot standby channel
function userStdbyChMessageCB(m){
	//robot is online
	if (m.status == "Available") {
		buttonConnected();
	}
	if (m.status == "Offline") {
		buttonDisconnected();
	}
}

function buttonConnected(){
	var button = document.getElementById("robotname_submit");
	button.className = "btn btn-success";
	button.innerHTML = "Disconnect";
	button.onclick = disconnectRobot;
}

function buttonDisconnected(){
	var button = document.getElementById("robotname_submit");
	button.className = "btn btn-default";
	button.innerHTML = "Connect";
	button.onclick = connectRobot;
}


// Will format in 12-hour h:mm.s a time format
function formatTime(millis) {
	var d = new Date(millis);
	var h = d.getHours();
	var m = d.getMinutes();
	var s = d.getSeconds();
	var a = (Math.floor(h / 12) === 0) ? "am" : "pm";
	return (h % 12) + ":" + m + "." + s + " " + a;
}

// Validate user input, allow only lowercace a-z and 0-9
function safeId(text) {
	return ('' + text).replace(/[^a-z0-9]/gi, '').toLocaleLowerCase();
}
//XSS Prevent
function safetxt(text) {
    return (''+text).replace( /[<>]/g, '' );
}