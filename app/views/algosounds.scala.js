@(char_id: String)(implicit req: RequestHeader)

var sounds = function() {
    buzz.defaults.formats = [ 'ogg', 'mp3' ];

    var phrases = {
        elephant: new buzz.sound("@routes.Assets.at("sounds/elephant")"),
        welcome: new buzz.sound("@routes.Assets.at("sounds/Welcome_Robotic")"),
        oldHA: new buzz.sound("@routes.Assets.at("sounds/HeavyAssault_Robotic")"),
        oldMEDIC: new buzz.sound("@routes.Assets.at("sounds/Medic_Robotic")"),
        oldLA: new buzz.sound("@routes.Assets.at("sounds/LightAssault_Robotic")"),
        oldENGY: new buzz.sound("@routes.Assets.at("sounds/Engineer_Robotic")"),
        oldINF: new buzz.sound("@routes.Assets.at("sounds/Infiltrator_Robotic")"),
        oldMAX: new buzz.sound("@routes.Assets.at("sounds/MAX_Robotic")")
    };

    var remaining = [];
    function say(phraseArray) {
        remaining = phraseArray;
        doSayPhrases();
    }

    function doSayPhrases() {
        if(remaining.length > 0 ) {
            if(window.chrome) { remaining[0].load(); console.log("Reloaded?")}
            remaining[0].bindOnce("ended",function(){
                remaining = remaining.slice(1,remaining.length);
                doSayPhrases();
            }).play();
        }
    }
    return {
        say:say,
        phrases:phrases
    }

}();