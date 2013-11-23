@(char_id: String)(implicit req: RequestHeader)

$(function() {

  buzz.defaults.formats = [ 'ogg', 'mp3' ];

  var elephant = new buzz.sound("@routes.Assets.at("sounds/elephant")");
  var soundHA = new buzz.sound("@routes.Assets.at("sounds/HeavyAssault_Robotic")");
  var soundMEDIC = new buzz.sound("@routes.Assets.at("sounds/Medic_Robotic")");
  var soundLA = new buzz.sound("@routes.Assets.at("sounds/LightAssault_Robotic")");
  var soundENGY = new buzz.sound("@routes.Assets.at("sounds/Engineer_Robotic")");
  var soundINF = new buzz.sound("@routes.Assets.at("sounds/Infiltrator_Robotic")");
  var soundWELCOME = new buzz.sound("@routes.Assets.at("sounds/Welcome_Robotic")");

  console.log(jsRoutes);

  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
  var algosocket = new WS("@routes.Application.thealgorithm.webSocketURL()")

  var receiveEvent = function(event) {
      var wat = $.parseJSON(event.data)
      console.log("EVENT -- " + wat);
      $.get("@routes.Application.squadInfo(char_id)",function(data) {
          $(".jumbotron").html(" " +
              "<h2>Your Default Role (Be this class): <b>"+data["role"]+"</b></h2>" +
              "<h2>Your Leader (Follow this guy): <b>"+data["leader"]+"</b></h2>");
          $("#squad ul").html("");
          $.each(data.assignments[0],function(index,value) {
              if(data.leader_id != @char_id) {
                  if(value.online) {
                    $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:green'>Online</span>)</li>"));
                  } else {
                    $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:red'>Offline</span>)</li>"));
                  }
              } else {
                  if(value.online) {
                      $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:green'>Online</span>) -- <a href='#' class='remove_mem' data-cid='"+value.id+"'>Remove</a> -- <a href='#' class='make_leader' data-cid='"+value.id+"'>Make Leader</a></li>"));
                  } else {
                      $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (<span style='color:red'>Offline</span>) -- <a href='#' class='remove_mem' data-cid='"+value.id+"'>Remove</a> -- <a href='#' class='make_leader' data-cid='"+value.id+"'>Make Leader</a></li>"));
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
        if(wat.role == "Heavy Assault") { soundHA.play() }
        if(wat.role == "Medic") { soundMEDIC.play() }
        if(wat.role == "Light Assault") { soundLA.play() }
        if(wat.role == "Engineer") { soundENGY.play() }
        if(wat.role == "Infiltrator") { soundINF.play() }
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

