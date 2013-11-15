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

  algosocket.onmessage = receiveEvent;

});

