@()

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
        oldMAX: new buzz.sound("@routes.Assets.at("sounds/MAX_Robotic")"),
        driver: new buzz.sound("@routes.Assets.at("sounds/Driver")"),
        engy: new buzz.sound("@routes.Assets.at("sounds/Engineer")"),
        team1: new buzz.sound("@routes.Assets.at("sounds/FireTeam1")"),
        team2: new buzz.sound("@routes.Assets.at("sounds/FireTeam2")"),
        team3: new buzz.sound("@routes.Assets.at("sounds/FireTeam3")"),
        flash: new buzz.sound("@routes.Assets.at("sounds/Flash")"),
        galaxy: new buzz.sound("@routes.Assets.at("sounds/Galaxy")"),
        gunner: new buzz.sound("@routes.Assets.at("sounds/Gunner")"),
        harasser: new buzz.sound("@routes.Assets.at("sounds/Harasser")"),
        ha: new buzz.sound("@routes.Assets.at("sounds/HeavyAssault")"),
        inf: new buzz.sound("@routes.Assets.at("sounds/Inflitrator")"),
        lib: new buzz.sound("@routes.Assets.at("sounds/Liberator")"),
        scythe: new buzz.sound("@routes.Assets.at("sounds/Scythe")"),
        la: new buzz.sound("@routes.Assets.at("sounds/LightAssault")"),
        lightning: new buzz.sound("@routes.Assets.at("sounds/Lightning")"),
        magrider: new buzz.sound("@routes.Assets.at("sounds/Magrider")"),
        max: new buzz.sound("@routes.Assets.at("sounds/Max")"),
        medic: new buzz.sound("@routes.Assets.at("sounds/Medic")"),
        new_role: new buzz.sound("@routes.Assets.at("sounds/NewRole")"),
        pull: new buzz.sound("@routes.Assets.at("sounds/Pull")"),
        recall: new buzz.sound("@routes.Assets.at("sounds/Recall")"),
        role: new buzz.sound("@routes.Assets.at("sounds/Role")"),
        scythe: new buzz.sound("@routes.Assets.at("sounds/Scythe")"),
        selected: new buzz.sound("@routes.Assets.at("sounds/SelectedToLead")"),
        new_leader: new buzz.sound("@routes.Assets.at("sounds/NewLeader_Robotic")"),
        sundy: new buzz.sound("@routes.Assets.at("sounds/Sunderer")")
    };

    var remaining = [];

    function say(phrase) {
        var len = remaining.length;
        if(phrase instanceof Array) {
            remaining = remaining.concat(phrase);
        } else {
            remaining.push(phrase);
        }
        if(len == 0) {
            doSayPhrases();
        }
    }

    function doSayPhrases() {
        if(remaining.length > 0 ) {
            if(window.chrome) { remaining[0].load();}
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
