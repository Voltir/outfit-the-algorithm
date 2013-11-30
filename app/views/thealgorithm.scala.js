@(char_id: String)(implicit req: RequestHeader)
@import models._

$(function() {
    console.log(jsRoutes)
  //Templates
  var squadSource = $("#squad-template").html();
  var squadTemplate = Handlebars.compile(squadSource);

  var role_sounds = {
    "@{Roles.HA}": sounds.phrases.ha,
    "@{Roles.MEDIC}": sounds.phrases.medic,
    "@{Roles.ENGY}": sounds.phrases.engy,
    "@{Roles.LA}": sounds.phrases.la,
    "@{Roles.INF}": sounds.phrases.inf,
    "@{Roles.MAX}": sounds.phrases.max
  };

  var fireteam_sounds = {
    "@{Fireteams.ONE}": sounds.phrases.team1,
    "@{Fireteams.TWO}": sounds.phrases.team2,
    "@{Fireteams.THREE}": sounds.phrases.team3,
    "@{Fireteams.DRIVER}": sounds.phrases.driver,
    "@{Fireteams.GUNNER}": sounds.phrases.gunner
  };

  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
  var algosocket = new WS("@routes.Application.thealgorithm(char_id).webSocketURL()")
  var current_role = sounds.phrases.elephant;
  var current_fireteam = sounds.phrases.elephant;
  var current_leader = "";

  if (annyang) {
    var commands = {
      'command role' : function() { console.log("ROLE"); sounds.say([sounds.phrases.role,current_role,current_fireteam]); },
      'command test' : function() { console.log("TEST"); elephant.play(); },
      'command rally' : function() { console.log("GATHER"); elephant.play(); },
      'pattern standard' : function() {
        console.log("STANDARD"); 
        algosocket.send(JSON.stringify({set_standard: "@char_id"}));
      },
      'pattern support' : function() {
        console.log("SUPPORT"); 
        algosocket.send(JSON.stringify({set_support: "@char_id"}));
      },
      'pattern jetpack' : function() {
        console.log("JETPACK");
        algosocket.send(JSON.stringify({set_jetpack: "@char_id"}));
      }
    };

    // Initialize annyang with our commands
    annyang.init(commands);

    // Start listening.
    annyang.start();
  }
  
  function GetSquadData() {
    $.get("@routes.Application.squadInfo(char_id)",function(data) {
          
      console.log(data);
          
      @*hackery to reset individual members who are removed from squad -- FIX THIS*@
      if(!data.my_assignment) {
        window.location = jsRoutes.controllers.Application.indexNoAuto().url;
      }
          
      $(".jumbotron").html(" " +
        "<h2>Your Default Role (Be this class): <b>"+data.my_assignment.role+"</b></h2>" +
        "<h2>Your Fireteam (Listen for this): <b>"+data.my_assignment.fireteam+"</b></h2>"+
        "<h2>Your Leader (Follow this guy): <b>"+data.leader+"</b></h2>");

        if(data.leader != current_leader && current_leader != "") {
          sounds.say([sounds.phrases.new_leader]);
          current_leader = data.leader;
        }
        if(current_leader == "") {
          current_leader = data.leader;
        }
        current_role = role_sounds[data.my_assignment.role];
        console.log(current_role);
        if(data.leader_id == "@char_id") {
          data.is_leader = true
        }
        $("#squads").html(squadTemplate(data));

    });
  }

  var receiveEvent = function(event) {
      var wat = $.parseJSON(event.data)
      console.log(wat);
      if(wat.command) {
          window.location = jsRoutes.controllers.Application.indexNoAuto().url;
      }
      GetSquadData();
      if(wat.role_change == "@char_id") {
        current_role = role_sounds[wat.assignment.role];
        current_fireteam = fireteam_sounds[wat.assignment.fireteam];
        sounds.say([sounds.phrases.new_role,current_role,current_fireteam]);
      }
  }

  $("#reset").click(function () {
    algosocket.send(JSON.stringify({command: "reset"}));
  });

  $("#take_leader").click(function() {
    algosocket.send(JSON.stringify({leaderize:"@char_id"}));
    GetSquadData();
  });

  $("#change").click(function () {
    algosocket.send(JSON.stringify({change: "change"}));
    GetSquadData();
  });

  $(".content").on("click",".remove_mem",function () {
    var cid = $(this).attr("data-cid");
    algosocket.send(JSON.stringify({remove:cid}));
    GetSquadData();
  });

  $(".content").on("click",".make_leader",function () {
    var cid = $(this).attr("data-cid");
    algosocket.send(JSON.stringify({leaderize:cid}));
    GetSquadData();
  });

  algosocket.onmessage = receiveEvent;

  sounds.say([sounds.phrases.welcome]);

  GetSquadData();
});

