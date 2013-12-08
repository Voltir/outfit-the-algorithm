@(char_id: String)(implicit req: RequestHeader)
@import models._

$(function() {
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

  var WS = WebSocket
  var algosocket = new WS("@routes.Application.thealgorithm(char_id).webSocketURL()")

  var welcome = true;
  var say_role = true;

  var current_role = {
      label: "",
      sound: sounds.phrases.elephant
  };

  var current_fireteam = {
    label: "",
    sound: sounds.phrases.elephant
  };

  var current_leader = "";

  if (annyang) {
    var commands = {
      'command role' : function() {
          console.log("ROLE");
          sounds.say([sounds.phrases.role,current_role.sound,current_fireteam.sound]);
      },
      'command roll' : function() {
          console.log("ROLL");
          sounds.say([sounds.phrases.role,current_role.sound,current_fireteam.sound]);
      },
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
      },
      'pattern crash' : function() {
        console.log("CRASH");
        algosocket.send(JSON.stringify({set_crash: "@char_id"}));
      }
    };

    // Initialize annyang with our commands
    annyang.init(commands);

    // Start listening.
    annyang.start();
  }
  
  function GetSquadData() {
    $.get("@routes.Application.squadInfo(char_id)",function(data) {
      var sounds_to_play = []
      console.log(data);

      if(welcome) {
          welcome = false;
          sounds_to_play.push(sounds.phrases.welcome)
      }

      @*hackery to reset individual members who are removed from squad -- FIX THIS*@
      if(!data.my_assignment) {
        window.location = jsRoutes.controllers.Application.indexNoAuto().url;
      }
          
      $(".jumbotron").html(" " +
        "<h2>Your Default Role (Be this class): <b>"+data.my_assignment.role+"</b></h2>" +
        "<h2>Your Fireteam (Listen for this): <b>"+data.my_assignment.fireteam+"</b></h2>"+
        "<h2>Your Leader (Follow this guy): <b>"+data.leader+"</b></h2>");

      if(say_role) {
        current_role = {
          label: data.my_assignment.role,
          sound: role_sounds[data.my_assignment.role]
        };
        sounds_to_play.push(sounds.phrases.new_role);
        sounds_to_play.push(current_role.sound);
      }

      if(say_role) {
        current_fireteam = {
          label: data.my_assignment.fireteam,
          sound: fireteam_sounds[data.my_assignment.fireteam]
        };
        sounds_to_play.push(current_fireteam.sound);
      }

      if(data.leader != current_leader && current_leader != "") {
        sounds_to_play.push(sounds.phrases.new_leader);
        current_leader = data.leader;
      }

      if(current_leader == "") {
        current_leader = data.leader;
      }

      if(data.leader_id == "@char_id") {
        data.is_leader = true
      }

      if(say_role) { say_role = false; }

      $("#squads").html(squadTemplate(data));

      if(sounds_to_play.length > 0 ) {
        sounds.say(sounds_to_play);
      }
    });
  }

  var receiveEvent = function(event) {
    var cmd = $.parseJSON(event.data)
    console.log(cmd);
    if(cmd.command) {
        window.location = jsRoutes.controllers.Application.indexNoAuto().url;
    }

    if(cmd.role_change && cmd.role_change == "@char_id") {
        say_role = true;
    }

    GetSquadData();
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

  GetSquadData();
});

