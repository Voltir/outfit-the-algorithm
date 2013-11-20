@(char_id: String)(implicit req: RequestHeader)
$(function() {
  console.log(jsRoutes);
  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
  var algosocket = new WS("@routes.Application.thealgorithm.webSocketURL()")

  var receiveEvent = function(event) {
      var wat = $.parseJSON(event.data)
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
      });
      if(wat.command) {
          window.location = jsRoutes.controllers.Application.index().url;
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

