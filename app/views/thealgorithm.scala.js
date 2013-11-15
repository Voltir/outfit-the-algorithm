@()(implicit req: RequestHeader)
$(function() {
  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
  var algosocket = new WS("@routes.Application.thealgorithm.webSocketURL()")

  var receiveEvent = function(event) {
    console.log("Yay!");
    console.log(event);
  }

  $("#testevent").click(function () {
    algosocket.send(JSON.stringify(
      {
        testevent: $("#testevent").val()
      }
    ));
    console.log("Test Event...");
  });

  $("#lookup").autocomplete({
    source: function(req,add) {
      var suggestions = [];
      $.get("lookup/"+req.term, function(data) {
        $.each(data[0], function(i,val){
            suggestions.push(val.name);
        });
        add(suggestions);
      });
    }
  });

  $("#register").click(function(){
     var character = $("#lookup").val();
     window.location = jsRoutes.controllers.Application.confirmCharacter(character)
  });

  algosocket.onmessage = receiveEvent;
});

