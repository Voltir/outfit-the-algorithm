@(char_id: String)(implicit req: RequestHeader)
$(function() {
  console.log(jsRoutes);
  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
  var algosocket = new WS("@routes.Application.thealgorithm.webSocketURL()")

  var receiveEvent = function(event) {
      var wat = $.parseJSON(event.data)
      console.log(wat);
      console.log(wat);
      console.log(wat);
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
      }
  }

  $("#reset").click(function () {
    algosocket.send(JSON.stringify({command: "reset"}));
  });

  $("#change").click(function () {
    algosocket.send(JSON.stringify({change: "change"}));
  });

  if (annyang) {
      // Let's define a command.
    var commands = {
      'algorithm regroup': function() { console.log("Regroup!"); }
      'algorithm set standard': function() { console.log("Set Standard!"); }
      'algorithm set max': function() { console.log("Set Max!"); }
    };

        // Initialize annyang with our commands
        annyang.init(commands);

          // Start listening.
          annyang.start();
  }
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

