import React, { Component } from 'react';
import Toolbar from 'react-md/lib/Toolbars';
import './App.scss';
import Helmet from 'react-helmet';
import TextField from 'react-md/lib/TextFields';
import Button from 'react-md/lib/Buttons/Button';
import List from 'react-md/lib/Lists/List';
import ListItem from 'react-md/lib/Lists/ListItem';
import moment from "moment";

class Message extends Component {
  render() {
    const msg = this.props.message;
    return (
       <ListItem primaryText={msg.message} secondaryText={msg.date}/>
    )
  }
}

class MessageInput extends Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleKeyPressed = this.handleKeyPressed.bind(this);

  }

  handleKeyPressed(event) {
    if(event.which === 13) {
       this.handleSubmit(event);
       return false;
    }
  }

  handleChange(newValue) {
    this.setState({value: newValue});
  }

  handleSubmit(event) {
    event.preventDefault();
    if (this.state.value === '') return;
    if (this.props.onMessageEntered != null && this.props.onMessageEntered(this.state.value)) {
      this.setState({value: ""});
    }
  }

  render() {
    return (
      <form className="md-grid" onSubmit={this.handleSubmit}>
        <TextField
            id="msg"
            label="chat message"
            value={this.state.value}
            className="md-cell md-cell--6"
            onChange={this.handleChange}
            onKeyDown={this.handleKeyPressed} />,
        <Button
          raised
          secondary
          label="Send"
          onClick={this.handleSubmit}
          className="md-cell md-cell--2 md-cell--middle">
          chat_bubble_outline
        </Button>
      </form>
    );
  }
}

class App extends Component {

  constructor(props) {
      super(props);
      this.onMessageEntered = this.onMessageEntered.bind(this);
      props.messanger.on({
        messageRecieved: this.messageRecieved.bind(this),
        stateChanged: this.stateChanged.bind(this)
      });

      this.nMessage = 0;

      this.state = {
        messages : []
      };
  }

  messageRecieved(messageText) {
    this.nMessage++;

    console.log(messageText);

    var now = moment(new Date());

    const newMsg = {
      id: ''+this.nMessage,
      date: now.format("HH:mm:ss"),
      message:messageText
    };
    this.setState((prevState) => ({
      messages: [newMsg].concat(prevState.messages),
    }));
  }

  stateChanged(state) {
    console.log(state);
  }

  onMessageEntered(msg) {
    this.props.messanger.chatMessage(msg);
    return true;
  }

  render() {

    return (
      <div>
        <Helmet title="Chat" />
        <Toolbar colored title="Chat" />
        <MessageInput onMessageEntered={this.onMessageEntered} />
        <div className="md-grid">
          <List className="md-cell md-cell--8 md-paper md-paper--1">
            {this.state.messages.map(function(msg) {
                return  ( <Message key={msg.id} message={msg} /> );
            })}
          </List>
        </div>
      </div>
    );
  }
}

export default App;
