
import SockJS from 'sockjs-client';

class SockJSMessanger {
  reconnectInterval = null;

  constructor(url) {
    this.url = url;
    this.state = "DISCONNECTED";
    this.reconnect();
  }

  on(handlers) {
    this.handlers = handlers;
  }

  chatMessage(msg) {
      if (this.sock != null) {
         this.sock.send(msg);
         return true;
      }
      return false;
  }

  reconnect() {
      const sock = new SockJS(this.url);
      var self = this;
      sock.onopen = function() { self.onConnectOpen(sock); };
      sock.onmessage = this.onMessage.bind(this);
      sock.onclose = this.onConnectClose.bind(this);
  }

  onConnectOpen(sock) {
    this.sock = sock;
    if (this.reconnectInterval != null) {
      this.changeState("RECONNECTED");
      clearInterval(this.reconnectInterval);
    } else {
      this.changeState("CONNECTED");
    }
  }

  onMessage(msg) {
    if (this.handlers.messageRecieved != null) {
      this.handlers.messageRecieved(msg.data);
    }
  }

  onConnectClose() {
    this.changeState("DISCONNECTED");
    this.sock = null;
    if (this.reconnectInterval != null) {
      clearInterval(this.reconnectInterval);
    }
    var self = this;
    this.reconnectInterval = setInterval(function() {
      self.reconnect();
    }, 2000);
  }

  changeState(newState) {
    if (this.handlers.stateChanged != null) {
      this.handlers.stateChanged(newState);
    }
  }
}

class DummyMessanger {
  constructor(handlers) {
    this.handlers = handlers;
    this.handlers.stateChanged("CONNECTED");
  }

  chatMessage(msg) {
    this.handlers.messageRecieved(msg);
  }
}


export default SockJSMessanger;
