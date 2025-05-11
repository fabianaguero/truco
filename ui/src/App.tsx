import React, { useEffect, useState } from 'react';
import { Layout, Tabs, Spin, message } from 'antd';
import JugadoresTab from './components/JugadoresTab';
import EquiposTab from './components/EquiposTab';
import { equipoService, jugadorService } from './services/api';

const { Header, Content } = Layout;

const App: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [activeKey, setActiveKey] = useState('1');

    useEffect(() => {
        console.log('App montado');
        verificarConexion();
    }, []);

    const verificarConexion = async () => {
        setLoading(true);
        try {
            await Promise.all([
                jugadorService.listar(),
                equipoService.listar()
            ]);
            console.log('Conexión con el backend establecida');
        } catch (error) {
            console.error('Error de conexión:', error);
            message.error('Error de conexión con el servidor');
        } finally {
            setLoading(false);
        }
    };

    console.log('Renderizando App');

    const items = [
        {
            key: '1',
            label: 'Jugadores',
            children: <JugadoresTab />
        },
        {
            key: '2',
            label: 'Equipos',
            children: <EquiposTab />
        }
    ];

    const handleTabChange = (key: string) => {
        console.log('Tab seleccionada:', key);
        setActiveKey(key);
    };

    return (
        <Layout style={{ minHeight: '100vh' }}>
            <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h1>Gestión de Jugadores y Equipos</h1>
                {loading && <Spin />}
            </Header>
            <Content style={{ padding: '24px' }}>
                <Tabs
                    activeKey={activeKey}
                    items={items}
                    onChange={handleTabChange}
                    destroyInactiveTabPane
                    style={{ background: '#fff', padding: '16px', borderRadius: '4px' }}
                />
            </Content>
        </Layout>
    );
};

export default App;