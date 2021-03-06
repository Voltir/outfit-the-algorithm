// Generated by CoffeeScript 1.6.3
(function() {
  var Dragster,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

  Dragster = (function() {
    function Dragster(el) {
      this.el = el;
      this.dragleave = __bind(this.dragleave, this);
      this.dragenter = __bind(this.dragenter, this);
      if (this.supportsEventConstructors()) {
        this.first = false;
        this.second = false;
        this.el.addEventListener("dragenter", this.dragenter, false);
        this.el.addEventListener("dragleave", this.dragleave, false);
        this.dragsterEnterEvent = new CustomEvent('dragster:enter', {
          bubbles: true,
          cancelable: true
        });
        this.dragsterLeaveEvent = new CustomEvent('dragster:leave', {
          bubbles: true,
          cancelable: true
        });
      }
    }

    Dragster.prototype.dragenter = function(event) {
      if (this.first) {
        return this.second = true;
      } else {
        this.first = true;
        return this.el.dispatchEvent(this.dragsterEnterEvent);
      }
    };

    Dragster.prototype.dragleave = function(event) {
      if (this.second) {
        this.second = false;
      } else if (this.first) {
        this.first = false;
      }
      if (!this.first && !this.second) {
        return this.el.dispatchEvent(this.dragsterLeaveEvent);
      }
    };

    Dragster.prototype.removeListeners = function() {
      this.el.removeEventListener("dragenter", this.dragenter, false);
      return this.el.removeEventListener("dragleave", this.dragleave, false);
    };

    Dragster.prototype.supportsEventConstructors = function() {
      try {
        new CustomEvent("z");
      } catch (_error) {
        return false;
      }
      return true;
    };

    return Dragster;

  })();

  window.Dragster = Dragster;

}).call(this);
