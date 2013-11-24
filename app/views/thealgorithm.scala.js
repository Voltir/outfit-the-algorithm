@(char_id: String)(implicit req: RequestHeader)
  

$(function() {

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

  if (annyang) {
    // Let's define a command.
    var commands = {
      'algo role' : function() { console.log("ROLE"); current_role.play(); },
      'algo repeat' : function() { console.log("REPEAT"); elephant.play(); },
      'algo gather' : function() { console.log("GATHER"); elephant.play(); },
      'algo standard' : function() { 
        console.log("STANDARD"); 
        algosocket.send(JSON.stringify({set_standard: "@char_id"}));
      },
      'algo support' : function() { 
        console.log("SUPPORT"); 
        algosocket.send(JSON.stringify({set_support: "@char_id"}));
      },
      'algo jetpack' : function() { 
        console.log("JETPACK"); 
        algosocket.send(JSON.stringify({set_jetpack: "@char_id"}));
      },
    };

    // Initialize annyang with our commands
    annyang.init(commands);

    // Start listening.
    annyang.start();
  }

  var receiveEvent = function(event) {
      var wat = $.parseJSON(event.data)
      console.log("EVENT -- " + wat);
      $.get("@routes.Application.squadInfo(char_id)",function(data) {
          $(".jumbotron").html(" " +
              "<h2>Your Default Role (Be this class): <b>"+data["role"]+"</b></h2>" +
              "<h2>Your Leader (Follow this guy): <b>"+data["leader"]+"</b></h2>");
          $("#squad ul").html("");
          $.each(data.assignments[0],function(index,value) {
              var resources = "[Air:??? / Armor:??? / Infantry:???]";
              if(value.resources) {
                resources = "[Air:"+value.resources.air+" / Armor:"+value.resources.armor+" / Infantry:"+value.resources.infantry+"]";
              }
              if(data.leader_id != @char_id) {
                  if(value.online) {
                    $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:green'>Online "+resources+"</span>)</li>"));
                  } else {
                    $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:red'>Offline "+resources+"</span>)</li>"));
                  }
              } else {
                  if(value.online) {
                      $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:green'>Online "+resources+"</span>) -- <a href='#' class='remove_mem' data-cid='"+value.id+"'>Remove</a> -- <a href='#' class='make_leader' data-cid='"+value.id+"'>Make Leader</a></li>"));
                  } else {
                      $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:red'>Offline "+resources+"</span>) -- <a href='#' class='remove_mem' data-cid='"+value.id+"'>Remove</a> -- <a href='#' class='make_leader' data-cid='"+value.id+"'>Make Leader</a></li>"));
                  }
              }
          });
          @*hackery to reset individual members who are removed from squad -- FIX THIS*@
          if(!data["role"]) {
            window.location = jsRoutes.controllers.Application.index().url;
          }
          if(data.leader_id == @char_id) {
              console.log("Todo... leader menu or something");
          }
      });
      if(wat.command) {
          window.location = jsRoutes.controllers.Application.index().url;
      }
      if(wat.role_change == "@char_id") {
        console.log(wat.role);
        if(wat.role == "Heavy Assault") { soundHA.play(); current_role = soundHA; }
        if(wat.role == "Medic") { soundMEDIC.play(); current_role = soundMENDIC; }
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

