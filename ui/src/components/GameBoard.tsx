import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Card as AntCard, Button, Space, Typography, Row, Col, message, Spin, Alert, Input, Modal, Form } from 'antd';
import { ReloadOutlined, PlayCircleOutlined } from '@ant-design/icons';
import CardComponent from './Card';
import { gameService } from '../services/api';
import { webSocketService } from '../services/websocket';
import type { Carta, ManoJugador, Jugada, WebSocketMessage } from '../types';

const { Title, Text } = Typography;

interface GameBoardProps {
  partidaId?: string;
  jugadorNombre?: string;
}

const GameBoard: React.FC<GameBoardProps> = ({ partidaId: propPartidaId, jugadorNombre: propJugadorNombre }) => {
  const [partidaId, setPartidaId] = useState<string>(propPartidaId || '');
  const [jugadorNombre, setJugadorNombre] = useState<string>(propJugadorNombre || '');
  const [gameState, setGameState] = useState<ManoJugador | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedCardIndex, setSelectedCardIndex] = useState<number | null>(null);
  const [wsConnected, setWsConnected] = useState(false);
  const [modalVisible, setModalVisible] = useState(true);
  const [form] = Form.useForm();
  const wsConnectedRef = useRef(false);

  const fetchGameState = useCallback(async () => {
    if (!partidaId || !jugadorNombre) return;
    
    setLoading(true);
    try {
      const response = await gameService.obtenerMano(partidaId, jugadorNombre);
      setGameState(response.data);
    } catch (error) {
      console.error('Error fetching game state:', error);
      message.error('Error al cargar el estado del juego');
    } finally {
      setLoading(false);
    }
  }, [partidaId, jugadorNombre]);

  // WebSocket message handler
  const handleWebSocketMessage = useCallback((msg: WebSocketMessage) => {
    if (msg.partidaId === partidaId) {
      console.log('WebSocket message received:', msg);
      // Refresh game state on any update
      fetchGameState();
    }
  }, [partidaId, fetchGameState]);

  // Connect to WebSocket when game is loaded
  useEffect(() => {
    if (partidaId && jugadorNombre) {
      webSocketService.connect(partidaId, handleWebSocketMessage)
        .then(() => {
          setWsConnected(true);
          wsConnectedRef.current = true;
        })
        .catch(() => {
          console.warn('WebSocket connection failed, using polling');
          setWsConnected(false);
          wsConnectedRef.current = false;
        });

      fetchGameState();

      return () => {
        webSocketService.disconnect();
        setWsConnected(false);
        wsConnectedRef.current = false;
      };
    }
  }, [partidaId, jugadorNombre, handleWebSocketMessage, fetchGameState]);

  // Polling fallback when WebSocket is not connected
  useEffect(() => {
    if (!wsConnected && partidaId && jugadorNombre) {
      const interval = setInterval(fetchGameState, 3000);
      return () => clearInterval(interval);
    }
  }, [wsConnected, partidaId, jugadorNombre, fetchGameState]);

  const handleJoinGame = (values: { partidaId: string; jugadorNombre: string }) => {
    setPartidaId(values.partidaId);
    setJugadorNombre(values.jugadorNombre);
    setModalVisible(false);
  };

  const handleCardSelect = (index: number) => {
    if (!gameState?.esMiTurno) {
      message.warning('No es tu turno');
      return;
    }
    setSelectedCardIndex(selectedCardIndex === index ? null : index);
  };

  const handlePlayCard = async () => {
    if (selectedCardIndex === null) {
      message.warning('Selecciona una carta primero');
      return;
    }

    setLoading(true);
    try {
      await gameService.jugarCarta(partidaId, jugadorNombre, selectedCardIndex);
      setSelectedCardIndex(null);
      // Only fetch manually if WebSocket is not connected
      if (!wsConnectedRef.current) {
        await fetchGameState();
      }
      message.success('Carta jugada');
    } catch (error) {
      console.error('Error playing card:', error);
      message.error('Error al jugar la carta');
    } finally {
      setLoading(false);
    }
  };

  const handleCantarTruco = async () => {
    setLoading(true);
    try {
      await gameService.cantarTruco(partidaId, jugadorNombre);
      // Only fetch manually if WebSocket is not connected
      if (!wsConnectedRef.current) {
        await fetchGameState();
      }
      message.success('¡Truco!');
    } catch (error) {
      console.error('Error calling truco:', error);
      message.error('No puedes cantar truco ahora');
    } finally {
      setLoading(false);
    }
  };

  const handleCantarEnvido = async () => {
    setLoading(true);
    try {
      await gameService.cantarEnvido(partidaId, jugadorNombre);
      // Only fetch manually if WebSocket is not connected
      if (!wsConnectedRef.current) {
        await fetchGameState();
      }
      message.success('¡Envido!');
    } catch (error) {
      console.error('Error calling envido:', error);
      message.error('No puedes cantar envido ahora');
    } finally {
      setLoading(false);
    }
  };

  const handleQuerer = async () => {
    setLoading(true);
    try {
      await gameService.querer(partidaId, jugadorNombre);
      // Only fetch manually if WebSocket is not connected
      if (!wsConnectedRef.current) {
        await fetchGameState();
      }
      message.success('¡Quiero!');
    } catch (error) {
      console.error('Error accepting:', error);
      message.error('No puedes querer ahora');
    } finally {
      setLoading(false);
    }
  };

  const handleNoQuerer = async () => {
    setLoading(true);
    try {
      await gameService.noQuerer(partidaId, jugadorNombre);
      // Only fetch manually if WebSocket is not connected
      if (!wsConnectedRef.current) {
        await fetchGameState();
      }
      message.info('No quiero');
    } catch (error) {
      console.error('Error declining:', error);
      message.error('No puedes declinar ahora');
    } finally {
      setLoading(false);
    }
  };

  const handleIrseAlMazo = async () => {
    setLoading(true);
    try {
      await gameService.irseAlMazo(partidaId, jugadorNombre);
      // Only fetch manually if WebSocket is not connected
      if (!wsConnectedRef.current) {
        await fetchGameState();
      }
      message.info('Te fuiste al mazo');
    } catch (error) {
      console.error('Error going to mazo:', error);
      message.error('No puedes irte al mazo ahora');
    } finally {
      setLoading(false);
    }
  };

  // Join game modal
  if (modalVisible || !partidaId || !jugadorNombre) {
    return (
      <Modal
        title="Unirse a Partida"
        open={modalVisible || !partidaId || !jugadorNombre}
        footer={null}
        closable={false}
      >
        <Form form={form} onFinish={handleJoinGame} layout="vertical">
          <Form.Item
            name="partidaId"
            label="ID de Partida"
            rules={[{ required: true, message: 'Ingresa el ID de la partida' }]}
            initialValue={propPartidaId}
          >
            <Input placeholder="ej: 123e4567-e89b-12d3-a456-426614174000" />
          </Form.Item>
          <Form.Item
            name="jugadorNombre"
            label="Nombre del Jugador"
            rules={[{ required: true, message: 'Ingresa tu nombre' }]}
            initialValue={propJugadorNombre}
          >
            <Input placeholder="Tu nombre en el juego" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              <PlayCircleOutlined /> Unirse
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    );
  }

  return (
    <div style={{ padding: 24, minHeight: '100vh', backgroundColor: '#1a472a' }}>
      <Spin spinning={loading}>
        {/* Header with scores */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={8}>
            <AntCard size="small">
              <Title level={4} style={{ margin: 0 }}>Equipo 1</Title>
              <Text strong style={{ fontSize: 24 }}>{gameState?.puntosEquipo1 || 0}</Text>
            </AntCard>
          </Col>
          <Col span={8}>
            <AntCard size="small" style={{ textAlign: 'center' }}>
              <Space direction="vertical">
                <Text type="secondary">Estado: {gameState?.estadoRonda || 'Cargando...'}</Text>
                <Text type="secondary">
                  Turno: {gameState?.turnoActual || 'Cargando...'}
                </Text>
                {wsConnected ? (
                  <Alert message="Conectado en tiempo real" type="success" showIcon style={{ padding: '2px 8px' }} />
                ) : (
                  <Alert message="Modo polling" type="warning" showIcon style={{ padding: '2px 8px' }} />
                )}
              </Space>
            </AntCard>
          </Col>
          <Col span={8}>
            <AntCard size="small" style={{ textAlign: 'right' }}>
              <Title level={4} style={{ margin: 0 }}>Equipo 2</Title>
              <Text strong style={{ fontSize: 24 }}>{gameState?.puntosEquipo2 || 0}</Text>
            </AntCard>
          </Col>
        </Row>

        {/* Table / Played cards area */}
        <AntCard 
          title="Mesa de Juego" 
          style={{ marginBottom: 24, minHeight: 200, backgroundColor: '#2d5a3d' }}
          headStyle={{ backgroundColor: '#1a472a', color: '#fff', borderBottom: 'none' }}
          bodyStyle={{ backgroundColor: '#2d5a3d' }}
        >
          <div style={{ 
            display: 'flex', 
            flexWrap: 'wrap', 
            gap: 16, 
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: 140
          }}>
            {gameState?.cartasJugadas && gameState.cartasJugadas.length > 0 ? (
              gameState.cartasJugadas.map((jugada: Jugada, index: number) => (
                <div key={index} style={{ textAlign: 'center' }}>
                  <CardComponent carta={jugada.carta} disabled />
                  <Text style={{ display: 'block', marginTop: 4, color: '#fff' }}>
                    {jugada.jugadorNombre}
                  </Text>
                </div>
              ))
            ) : (
              <Text style={{ color: '#fff' }}>No hay cartas jugadas aún</Text>
            )}
          </div>
        </AntCard>

        {/* Player's hand */}
        <AntCard 
          title={`Tu mano (${jugadorNombre})`}
          extra={
            <Space>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={fetchGameState}
                size="small"
              >
                Actualizar
              </Button>
              {gameState?.esMiTurno && (
                <Text type="success" strong>¡Es tu turno!</Text>
              )}
            </Space>
          }
          style={{ marginBottom: 24 }}
        >
          <div style={{ display: 'flex', gap: 16, justifyContent: 'center', flexWrap: 'wrap' }}>
            {gameState?.cartas && gameState.cartas.length > 0 ? (
              gameState.cartas.map((carta: Carta, index: number) => (
                <CardComponent
                  key={index}
                  carta={carta}
                  onClick={() => handleCardSelect(index)}
                  selected={selectedCardIndex === index}
                  disabled={!gameState?.esMiTurno}
                />
              ))
            ) : (
              <Text>No tienes cartas</Text>
            )}
          </div>
        </AntCard>

        {/* Action buttons */}
        <AntCard title="Acciones">
          <Space wrap>
            <Button 
              type="primary" 
              onClick={handlePlayCard}
              disabled={selectedCardIndex === null || !gameState?.esMiTurno}
              size="large"
            >
              Jugar Carta
            </Button>
            <Button 
              type="default" 
              onClick={handleCantarTruco}
              disabled={!gameState?.esMiTurno}
              style={{ backgroundColor: '#fa8c16', borderColor: '#fa8c16', color: '#fff' }}
              size="large"
            >
              Truco
            </Button>
            <Button 
              type="default" 
              onClick={handleCantarEnvido}
              disabled={!gameState?.esMiTurno}
              style={{ backgroundColor: '#722ed1', borderColor: '#722ed1', color: '#fff' }}
              size="large"
            >
              Envido
            </Button>
            <Button 
              type="default" 
              onClick={handleQuerer}
              style={{ backgroundColor: '#52c41a', borderColor: '#52c41a', color: '#fff' }}
              size="large"
            >
              Quiero
            </Button>
            <Button 
              type="default" 
              onClick={handleNoQuerer}
              style={{ backgroundColor: '#f5222d', borderColor: '#f5222d', color: '#fff' }}
              size="large"
            >
              No Quiero
            </Button>
            <Button 
              type="default" 
              onClick={handleIrseAlMazo}
              danger
              size="large"
            >
              Mazo
            </Button>
          </Space>
        </AntCard>
      </Spin>
    </div>
  );
};

export default GameBoard;
