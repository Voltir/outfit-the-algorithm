@(char_id: String)(implicit req: RequestHeader)
  

$(function() {
    console.log(jsRoutes)
  //Templates
  var squadSource = $("#squad-template").html();
  var squadTemplate = Handlebars.compile(squadSource);

  buzz.defaults.formats = [ 'ogg', 'mp3' ];

  var elephant = new buzz.sound("@routes.Assets.at("sounds/elephant")");
  var soundHA = new buzz.sound("@routes.Assets.at("sounds/HeavyAssault_Robotic")");
  var soundMEDIC = new buzz.sound("@routes.Assets.at("sounds/Medic_Robotic")");
  var soundLA = new buzz.sound("@routes.Assets.at("sounds/LightAssault_Robotic")");
  var soundENGY = new buzz.sound("@routes.Assets.at("sounds/Engineer_Robotic")");
  var soundINF = new buzz.sound("@routes.Assets.at("sounds/Infiltrator_Robotic")");
  var soundMAX = new buzz.sound("@routes.Assets.at("sounds/MAX_Robotic")");
  var soundWELCOME = new buzz.sound("@routes.Assets.at("sounds/Welcome_Robotic")");

  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
  var algosocket = new WS("@routes.Application.thealgorithm(char_id).webSocketURL()")
  var current_role = elephant;

  @*sounds.say([sounds.phrases.elephant,sounds.phrases.oldHA,sounds.phrases.elephant]);*@

  if (annyang) {
    // Let's define a command.
    var commands = {
      'command assignment' : function() { console.log("ROLE"); current_role.play(); },
      'command test' : function() { console.log("REPEAT"); elephant.play(); },
      'command rally' : function() { console.log("GATHER"); elephant.play(); },
      'pattern standard' : function() {
        console.log("STANDARD"); 
        algosocket.send(JSON.stringify({set_standard: "@char_id"}));
      },
      'pattern support' : function() {
        console.log("SUPPORT"); 
        algosocket.send(JSON.stringify({set_support: "@char_id"}));
      },
      'pattern light assault' : function() {
        console.log("JETPACK");
        algosocket.send(JSON.stringify({set_jetpack: "@char_id"}));
      }
    };

    // Initialize annyang with our commands
    annyang.init(commands);

    // Start listening.
    annyang.start();
  }

  var receiveEvent = function(event) {
      var wat = $.parseJSON(event.data)
      console.log("EVENT -- ");
      console.log(wat);
      $.get("@routes.Application.squadInfo(char_id)",function(data) {
          $(".jumbotron").html(" " +
              "<h2>Your Default Role (Be this class): <b>"+data.my_assignment.role+"</b></h2>" +
              "<h2>Your Fireteam (Listen for this): <b>"+data.my_assignment.fireteam+"</b></h2>"+
              "<h2>Your Leader (Follow this guy): <b>"+data.leader+"</b></h2>");
          console.log(data);

          $("#squads").html(squadTemplate(data));

          @*hackery to reset individual members who are removed from squad -- FIX THIS*@
          if(!data.my_assignment.role) {
            window.location = jsRoutes.controllers.Application.index().url;
          }
      });
      if(wat.command) {
          window.location = jsRoutes.controllers.Application.index().url;
      }
      if(wat.role_change == "@char_id") {
        console.log(wat.role);
        if(wat.role == "Heavy Assault") { soundHA.play(); current_role = soundHA; }
        if(wat.role == "Medic") { soundMEDIC.play(); current_role = soundMEDIC; }
        if(wat.role == "Light Assault") { soundLA.play(); current_role = soundLA; }
        if(wat.role == "Engineer") { soundENGY.play(); current_role = soundENGY; }
        if(wat.role == "Infiltrator") { soundINF.play(); current_role = soundINF; }
        if(wat.role == "MAX") { soundMAX.play(); current_role = soundMAX; }
      }
  }

  $("#reset").click(function () {
    algosocket.send(JSON.stringify({command: "reset"}));
  });

  $("#change").click(function () {
    algosocket.send(JSON.stringify({change: "change"}));
  });

  $(".content").on("click",".remove_mem",function () {
    var cid = $(this).attr("data-cid");
    algosocket.send(JSON.stringify({remove:cid}));
  });

  $(".content").on("click",".make_leader",function () {
    var cid = $(this).attr("data-cid");
    algosocket.send(JSON.stringify({leaderize:cid}));
  });

  algosocket.onmessage = receiveEvent;

  soundWELCOME.play();
});

