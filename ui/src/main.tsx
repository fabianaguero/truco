import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import esES from 'antd/locale/es_ES';
import App from './App';

console.log('Iniciando aplicación...');
const root = document.getElementById('root');
console.log('Elemento root:', root);

if (root) {
    console.log('Montando React en el elemento root');
    ReactDOM.createRoot(root).render(
        <React.StrictMode>
            <ConfigProvider locale={esES}>
                <App />
            </ConfigProvider>
        </React.StrictMode>
    );
} else {
    console.error('No se encontró el elemento root');
}