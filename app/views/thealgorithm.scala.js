@(char_id: String)(implicit req: RequestHeader)
@import models._

$(function() {
  //Templates
  var squadSource = $("#squad-template").html();
  var unassignedSource = $("#unassigned-template").html();
  var squadTemplate = Handlebars.compile(squadSource);
  var unassignedTemplate = Handlebars.compile(unassignedSource);

  var role_sounds = {
    "@{Roles.HA}": sounds.phrases.ha,
    "@{Roles.MEDIC}": sounds.phrases.medic,
    "@{Roles.ENGY}": sounds.phrases.engy,
    "@{Roles.LA}": sounds.phrases.la,
    "@{Roles.INF}": sounds.phrases.inf,
    "@{Roles.MAX}": sounds.phrases.max,
    "@{Roles.LIGHTNING}": sounds.phrases.lightning,
    "@{Roles.HARASSER}": sounds.phrases.harasser,
    "@{Roles.MAG}": sounds.phrases.magrider

  };

  var fireteam_sounds = {
    "@{Fireteams.ONE}": sounds.phrases.team1,
    "@{Fireteams.TWO}": sounds.phrases.team2,
    "@{Fireteams.THREE}": sounds.phrases.team3,
    "@{Fireteams.DRIVER}": sounds.phrases.driver,
    "@{Fireteams.GUNNER}": sounds.phrases.gunner
  };

  var WS = WebSocket;
  @if(play.api.Play.isDev(play.api.Play.current)) {
  var stab = "@routes.Application.thealgorithm(char_id).webSocketURL()";
  } else {
  var stab = "wss"+"@routes.Application.thealgorithm(char_id).webSocketURL()".substr(2);
  }
  var algosocket = new WS(stab);

  var welcome = true;
  var say_role = true;
  var say_leader = false;
  var unassigned = true;

  var current_role = {
      label: "",
      sound: sounds.phrases.elephant
  };

  var current_fireteam = {
    label: "",
    sound: sounds.phrases.elephant
  };

  var current_leader = "";
  var current_squad_id = "";

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
      },
      'pattern tank' : function() {
        console.log("MAGRIDER");
        algosocket.send(JSON.stringify({set_magrider: "@char_id"}));
      },
      'pattern buggy' : function() {
        console.log("BUGGY");
        algosocket.send(JSON.stringify({set_buggy: "@char_id"}));
      },
      'pattern lightning' : function() {
        console.log("LIGHTNING");
        algosocket.send(JSON.stringify({set_lightning: "@char_id"}));
      },
      'pattern air' : function() {
        console.log("AIR");
        algosocket.send(JSON.stringify({set_air: "@char_id"}));
      },
      'pattern heavy' : function() {
        console.log("AIR");
        algosocket.send(JSON.stringify({set_heavy: "@char_id"}));
      },
      'pattern echo' : function() {
        console.log("ECHO");
        algosocket.send(JSON.stringify({set_echo: "@char_id"}));
      },
      'pattern foxtrot' : function() {
        console.log("FOXTROT");
        algosocket.send(JSON.stringify({set_foxtrot: "@char_id"}));
      }
    };

    // Initialize annyang with our commands
    annyang.init(commands);

    // Start listening.
    annyang.start();
  }
 

  function SortScoreSquadMember(member) {
    var score = 0;
    if(member.is_leader == false) { score += 100; }
    if(member.assignment.role == "@{Roles.HA}") { score += 1; }
    if(member.assignment.role == "@{Roles.MAX}") { score += 2; }
    if(member.assignment.role == "@{Roles.MEDIC}") { score += 3; }
    if(member.assignment.role == "@{Roles.ENGY}") { score += 4; }
    if(member.assignment.role == "@{Roles.LA}") { score += 5; }
    if(member.assignment.role == "@{Roles.INF}") { score += 6; }
    if(member.assignment.fireteam == "@{Fireteams.TWO}") { score += 1000; }
    if(member.assignment.fireteam == "@{Fireteams.THREE}") { score += 2000; }
    return score;
  };

  function GetSquadData() {
    $.ajax({
      url: "@routes.Application.squadInfo(char_id)",
      cache: false,
      success: function(data) {
      var sounds_to_play = [];

      if(welcome) {
          welcome = false;
          sounds_to_play.push(sounds.phrases.welcome);
      }

      if(data.my_assignment) {
        unassigned = false;
        $(".jumbotron").html(" " +
            "<h2>Your Default Role (Be this class): <b>"+data.my_assignment.role+"</b></h2>" +
            "<h2>Your Fireteam (Listen for this): <b>"+data.my_assignment.fireteam+"</b></h2>"+
            "<h2>Your Leader (Follow this guy): <b>"+data.my_squad.leader+"</b></h2>");
      } else {
          $(".jumbotron").html(" " + "<h2>Currently Unassigned...</h2>");
          unassigned = true;
          current_role = {
              label: "",
              sound: sounds.phrases.elephant
          };

          current_fireteam = {
              label: "",
              sound: sounds.phrases.elephant
          };
      }
      if(data.my_assignment && say_leader) {
        say_leader = false;
        current_leader = data.my_squad.leader;
        sounds_to_play.push(sounds.phrases.new_leader);
      }

      if(data.my_assignment && data.my_squad.leader != current_leader) {
        current_leader = data.my_squad.leader;
        sounds_to_play.push(sounds.phrases.new_leader);
      }

      if(data.my_assignment && (say_role || data.my_assignment.role != current_role.label) ) {
        current_role = {
          label: data.my_assignment.role,
          sound: role_sounds[data.my_assignment.role]
        };
        sounds_to_play.push(sounds.phrases.new_role);
        sounds_to_play.push(current_role.sound);
      }

      if(data.my_assignment && (say_role || data.my_assignment.fireteam != current_fireteam.label)) {
        current_fireteam = {
          label: data.my_assignment.fireteam,
          sound: fireteam_sounds[data.my_assignment.fireteam]
        };
        sounds_to_play.push(current_fireteam.sound);
      }

      if(data.my_assignment && data.my_squad.leader_id == "@char_id") {
        data.my_squad.is_leader = true;
        current_squad_id = data.my_squad.squad_id;
        $.each(data.other_squads, function(index,value) {
          data.other_squads[index].is_other_leader = true;
        });
      }
      if(say_role) { say_role = false; }

      var squads = data.other_squads;
      if(data.my_squad) {
          squads.unshift(data.my_squad);
      }

      //Sort order for squads members
      $.each(squads, function(index,value) {
        squads[index].members.sort(function(a,b) {
          return SortScoreSquadMember(a) - SortScoreSquadMember(b);
        });
      });
      data.squads = squads;
      data.is_unassigned = unassigned;
      
      $("#squads").html(squadTemplate(data));
      $("#unassigned").html(unassignedTemplate(data));
      if(sounds_to_play.length > 0 @*&& "@char_id" != "5428010618041120721"*@) {
        sounds.say(sounds_to_play);
      }
    }});
  }

  var receiveEvent = function(event) {
    var cmd = $.parseJSON(event.data)
    if(cmd.command) {
        window.location = jsRoutes.controllers.Application.indexNoAuto().url;
    }

    if(cmd.remove && cmd.remove == "@char_id") {
        window.location = jsRoutes.controllers.Application.indexNoAuto().url;
    }

    if(cmd.role_change && cmd.role_change == "@char_id") {
        say_role = true;
        if(unassigned) {
            say_leader = true;
        }
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

  $("#unassign_self").click(function() {
    algosocket.send(JSON.stringify({unassign:"@char_id"}));
    GetSquadData();
  });

  $("#leave").click(function() {
    algosocket.send(JSON.stringify({remove:"@char_id"}));
    GetSquadData();
  });

  $("#create_squad").click(function() {
    algosocket.send(JSON.stringify({create_squad:"@char_id"}));
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
  
  $(".content").on("click",".unassign_mem",function () {
    var cid = $(this).attr("data-cid");
    algosocket.send(JSON.stringify({unassign:cid}));
    GetSquadData();
  });
  
  $(".content").on("click",".take_mem",function () {
    var cid = $(this).attr("data-cid");
    var cmd = {
        command: "JOIN",
        squad_id: ""+current_squad_id,
        cid: cid
    };
    algosocket.send(JSON.stringify(cmd));
    GetSquadData();
  });

  $(".content").on("click",".join_squad",function () {
    var sid = $(this).attr("data-sid");
    var cmd = {
        command: "JOIN",
        squad_id: sid,
        cid: "@char_id"
    };
    algosocket.send(JSON.stringify(cmd));
    GetSquadData();
  });

  algosocket.onmessage = receiveEvent;

  GetSquadData();
});

