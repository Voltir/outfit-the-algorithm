@(char_id: String)(implicit req: RequestHeader)

$(function() {

  buzz.defaults.formats = [ 'ogg', 'mp3' ];

  var elephant = new buzz.sound("@routes.Assets.at("sounds/elephant")");
  var soundHA = new buzz.sound("@routes.Assets.at("sounds/elephant")");
  var soundMEDIC = new buzz.sound("@routes.Assets.at("sounds/elephant")");
  var soundLA = new buzz.sound("@routes.Assets.at("sounds/elephant")");
  var soundENGY = new buzz.sound("@routes.Assets.at("sounds/elephant")");
  var soundINF = new buzz.sound("@routes.Assets.at("sounds/elephant")");

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
              if(value.online) {
                $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (online)</li>"));
              } else {
                $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (offline)</li>"));
              }
          });
          @*hackery to reset individual members who are removed from squad -- FIX THIS*@
          if(!data["role"]) {
            window.location = jsRoutes.controllers.Application.index().url;
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

  algosocket.onmessage = receiveEvent;

    $.get("@routes.Application.squadInfo(char_id)",function(data) {
        $(".jumbotron").html(" " +
            "<h2>Your Default Role (Be this class): <b>"+data["role"]+"</b></h2>" +
            "<h2>Your Leader (Follow this guy): <b>"+data["leader"]+"</b></h2>");
        $("#squad ul").html("");
        $.each(data.assignments[0],function(index,value) {
            if(value.is_online) {
                $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (online)</li>"));
            } else {
                $("#squad ul").append($("<li><b>"+value.name+"</b>: "+value.role+" (offline)</li>"));
            }
    });
  });
});

