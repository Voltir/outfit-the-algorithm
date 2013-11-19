@()(implicit req: RequestHeader)
$(function() {
  console.log(jsRoutes);
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
            $("#lookup").data(val.name,val.cid.id)
            suggestions.push(val.name);
        });
        add(suggestions);
      });
    },
    select: function(event,ui) {
      console.log("Selected...");
      var character = $("#lookup").val();
      console.log(character);
      console.log($("#lookup").data(character))
    }
  });

  $("#register").click(function(){
     var character = $("#lookup").val();
     var character_id = $("#lookup").data(character);
     window.location = jsRoutes.controllers.Application.profile(character,character_id).url
  });

  algosocket.onmessage = receiveEvent;
});

