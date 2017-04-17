import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import WebFontLoader from 'webfontloader';
import Messanger from './Messanger.js';

WebFontLoader.load({
  google: {
    families: ['Roboto:300,400,500,700', 'Material Icons'],
  },
});

const messanger = new Messanger('/chat');

ReactDOM.render(
  <App messanger={messanger} />,
  document.getElementById('root')
);
