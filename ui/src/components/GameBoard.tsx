import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Card as AntCard, Button, Space, Typography, Row, Col, message, Spin, Alert, Input, Modal, Form, Select, Divider, Tabs } from 'antd';
import { ReloadOutlined, PlayCircleOutlined, PlusOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons';
import CardComponent from './Card';
import { gameService, equipoService } from '../services/api';
import { webSocketService } from '../services/websocket';
import type { Carta, ManoJugador, Jugada, WebSocketMessage, Equipo, PartidaState } from '../types';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

interface GameBoardProps {
  partidaId?: string;
  jugadorNombre?: string;
}

const GameBoard: React.FC<GameBoardProps> = ({ partidaId: propPartidaId, jugadorNombre: propJugadorNombre }) => {
  const [partidaId, setPartidaId] = useState<string>(propPartidaId || '');
  const [jugadorNombre, setJugadorNombre] = useState<string>(propJugadorNombre || '');
  const [gameState, setGameState] = useState<ManoJugador | null>(null);
  const [partidaInfo, setPartidaInfo] = useState<PartidaState | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedCardIndex, setSelectedCardIndex] = useState<number | null>(null);
  const [wsConnected, setWsConnected] = useState(false);
  const [modalVisible, setModalVisible] = useState(true);
  const [availableEquipos, setAvailableEquipos] = useState<Equipo[]>([]);
  const [selectedEquipos, setSelectedEquipos] = useState<string[]>([]);
  const [joinForm] = Form.useForm();
  const [createForm] = Form.useForm();
  const wsConnectedRef = useRef(false);

  // Load available equipos when modal opens
  useEffect(() => {
    if (modalVisible) {
      loadEquipos();
    }
  }, [modalVisible]);

  const loadEquipos = async () => {
    try {
      const response = await equipoService.listar();
      setAvailableEquipos(response.data);
    } catch (error) {
      console.error('Error loading equipos:', error);
      message.error('Error al cargar los equipos disponibles');
    }
  };

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

  const fetchPartidaInfo = useCallback(async () => {
    if (!partidaId) return;
    
    try {
      const response = await gameService.obtenerPartida(partidaId);
      setPartidaInfo(response.data);
    } catch (error) {
      console.error('Error fetching partida info:', error);
    }
  }, [partidaId]);

  // WebSocket message handler
  const handleWebSocketMessage = useCallback((msg: WebSocketMessage) => {
    if (msg.partidaId === partidaId) {
      console.log('WebSocket message received:', msg);
      // Refresh game state on any update
      fetchGameState();
      fetchPartidaInfo();
    }
  }, [partidaId, fetchGameState, fetchPartidaInfo]);

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
      fetchPartidaInfo();

      return () => {
        webSocketService.disconnect();
        setWsConnected(false);
        wsConnectedRef.current = false;
      };
    }
  }, [partidaId, jugadorNombre, handleWebSocketMessage, fetchGameState, fetchPartidaInfo]);

  // Polling fallback when WebSocket is not connected
  useEffect(() => {
    if (!wsConnected && partidaId && jugadorNombre) {
      const interval = setInterval(() => {
        fetchGameState();
        fetchPartidaInfo();
      }, 3000);
      return () => clearInterval(interval);
    }
  }, [wsConnected, partidaId, jugadorNombre, fetchGameState, fetchPartidaInfo]);

  const handleJoinGame = (values: { partidaId: string; jugadorNombre: string }) => {
    setPartidaId(values.partidaId);
    setJugadorNombre(values.jugadorNombre);
    setModalVisible(false);
  };

  const handleCreateGame = async (values: { partidaId: string; jugadorNombre: string; equipos: string[] }) => {
    if (values.equipos.length !== 2) {
      message.error('Debes seleccionar exactamente 2 equipos');
      return;
    }

    setLoading(true);
    try {
      // Get the selected equipos with their players
      const selectedEquipoData = availableEquipos.filter(e => values.equipos.includes(e.id));
      
      // Validate that both teams have players
      for (const equipo of selectedEquipoData) {
        if (!equipo.jugadores || equipo.jugadores.length === 0) {
          message.error(`El equipo "${equipo.nombre}" no tiene jugadores asignados`);
          setLoading(false);
          return;
        }
      }

      // Create the partida with the selected teams
      const equiposDTO = selectedEquipoData.map(e => ({
        nombre: e.nombre,
        jugadores: e.jugadores?.map(j => j.nombre) || []
      }));

      const response = await gameService.crearPartida({
        partidaId: values.partidaId,
        equiposAleatorios: false,
        equipos: equiposDTO
      });

      message.success('¡Partida creada exitosamente!');
      setPartidaId(response.data);
      setJugadorNombre(values.jugadorNombre);
      setModalVisible(false);
    } catch (error) {
      console.error('Error creating game:', error);
      message.error('Error al crear la partida');
    } finally {
      setLoading(false);
    }
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

  // Get team names and players from partidaInfo for display
  const getTeamInfo = (index: number) => {
    if (partidaInfo?.equipos && partidaInfo.equipos[index]) {
      const equipo = partidaInfo.equipos[index];
      return {
        nombre: equipo.nombre,
        jugadores: equipo.jugadores?.map(j => j.nombre).join(', ') || 'Sin jugadores',
        puntaje: equipo.puntaje
      };
    }
    return {
      nombre: `Equipo ${index + 1}`,
      jugadores: '',
      puntaje: index === 0 ? (gameState?.puntosEquipo1 || 0) : (gameState?.puntosEquipo2 || 0)
    };
  };

  // Get turn order information
  const getTurnOrder = () => {
    if (!partidaInfo?.equipos) return [];
    const allPlayers: string[] = [];
    partidaInfo.equipos.forEach(equipo => {
      equipo.jugadores?.forEach(j => allPlayers.push(j.nombre));
    });
    return allPlayers;
  };

  // Join/Create game modal
  if (modalVisible || !partidaId || !jugadorNombre) {
    return (
      <Modal
        title="Mesa de Juego"
        open={modalVisible || !partidaId || !jugadorNombre}
        footer={null}
        closable={false}
        width={600}
      >
        <Tabs defaultActiveKey="join">
          <TabPane tab={<span><PlayCircleOutlined /> Unirse a Partida</span>} key="join">
            <Form form={joinForm} onFinish={handleJoinGame} layout="vertical">
              <Form.Item
                name="partidaId"
                label="Nombre/ID de la Partida"
                rules={[{ required: true, message: 'Ingresa el nombre o ID de la partida' }]}
                initialValue={propPartidaId}
              >
                <Input placeholder="ej: Batalla de truco de los viernes" />
              </Form.Item>
              <Form.Item
                name="jugadorNombre"
                label="Tu Nombre de Jugador"
                rules={[{ required: true, message: 'Ingresa tu nombre' }]}
                initialValue={propJugadorNombre}
              >
                <Input prefix={<UserOutlined />} placeholder="Tu nombre en el juego" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" block>
                  <PlayCircleOutlined /> Unirse a la Partida
                </Button>
              </Form.Item>
            </Form>
          </TabPane>
          
          <TabPane tab={<span><PlusOutlined /> Crear Nueva Partida</span>} key="create">
            <Spin spinning={loading}>
              <Form form={createForm} onFinish={handleCreateGame} layout="vertical">
                <Form.Item
                  name="partidaId"
                  label="Nombre de la Partida"
                  rules={[{ required: true, message: 'Ingresa un nombre para la partida' }]}
                  extra="Elige un nombre descriptivo para identificar la partida"
                >
                  <Input placeholder="ej: Batalla de truco de los viernes" />
                </Form.Item>
                
                <Form.Item
                  name="jugadorNombre"
                  label="Tu Nombre de Jugador"
                  rules={[{ required: true, message: 'Ingresa tu nombre' }]}
                >
                  <Input prefix={<UserOutlined />} placeholder="Tu nombre en el juego" />
                </Form.Item>

                <Divider>Seleccionar Equipos</Divider>
                
                <Form.Item
                  name="equipos"
                  label="Equipos Participantes"
                  rules={[
                    { required: true, message: 'Selecciona los equipos' },
                    { 
                      validator: async (_, value) => {
                        if (value && value.length !== 2) {
                          throw new Error('Debes seleccionar exactamente 2 equipos');
                        }
                      }
                    }
                  ]}
                  extra="Selecciona exactamente 2 equipos para la partida"
                >
                  <Select
                    mode="multiple"
                    placeholder="Selecciona 2 equipos"
                    style={{ width: '100%' }}
                    onChange={(values) => setSelectedEquipos(values as string[])}
                    optionLabelProp="label"
                  >
                    {availableEquipos.map(equipo => (
                      <Select.Option 
                        key={equipo.id} 
                        value={equipo.id} 
                        label={equipo.nombre}
                        disabled={selectedEquipos.length >= 2 && !selectedEquipos.includes(equipo.id)}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span><TeamOutlined /> {equipo.nombre}</span>
                          <span style={{ color: '#888', fontSize: 12 }}>
                            {equipo.jugadores?.length || 0} jugadores
                          </span>
                        </div>
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>

                {/* Show selected teams preview */}
                {selectedEquipos.length > 0 && (
                  <AntCard size="small" title="Equipos Seleccionados" style={{ marginBottom: 16 }}>
                    {selectedEquipos.map(equipoId => {
                      const equipo = availableEquipos.find(e => e.id === equipoId);
                      if (!equipo) return null;
                      return (
                        <div key={equipoId} style={{ marginBottom: 8 }}>
                          <Text strong><TeamOutlined /> {equipo.nombre}</Text>
                          <br />
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            Jugadores: {equipo.jugadores?.map(j => j.nombre).join(', ') || 'Sin jugadores'}
                          </Text>
                        </div>
                      );
                    })}
                  </AntCard>
                )}

                {availableEquipos.length === 0 && (
                  <Alert 
                    message="No hay equipos disponibles" 
                    description="Primero debes crear equipos y asignarles jugadores en la pestaña 'Equipos'" 
                    type="warning" 
                    showIcon 
                    style={{ marginBottom: 16 }}
                  />
                )}

                <Form.Item>
                  <Button 
                    type="primary" 
                    htmlType="submit" 
                    block 
                    disabled={availableEquipos.length < 2}
                  >
                    <PlusOutlined /> Crear Partida
                  </Button>
                </Form.Item>
              </Form>
            </Spin>
          </TabPane>
        </Tabs>
      </Modal>
    );
  }

  const team1 = getTeamInfo(0);
  const team2 = getTeamInfo(1);
  const turnOrder = getTurnOrder();

  return (
    <div style={{ padding: 24, minHeight: '100vh', backgroundColor: '#1a472a' }}>
      <Spin spinning={loading}>
        {/* Game title */}
        <AntCard size="small" style={{ marginBottom: 16, textAlign: 'center' }}>
          <Title level={3} style={{ margin: 0 }}>
            🃏 {partidaId}
          </Title>
        </AntCard>

        {/* Header with scores and team info */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={8}>
            <AntCard size="small">
              <Title level={4} style={{ margin: 0 }}><TeamOutlined /> {team1.nombre}</Title>
              <Text type="secondary" style={{ fontSize: 12 }}>{team1.jugadores}</Text>
              <div style={{ marginTop: 8 }}>
                <Text strong style={{ fontSize: 24 }}>{team1.puntaje}</Text>
                <Text type="secondary"> puntos</Text>
              </div>
            </AntCard>
          </Col>
          <Col span={8}>
            <AntCard size="small" style={{ textAlign: 'center' }}>
              <Space direction="vertical" size="small">
                <Text type="secondary">Estado: {gameState?.estadoRonda || 'Cargando...'}</Text>
                <Text strong style={{ color: '#52c41a' }}>
                  Turno: {gameState?.turnoActual || 'Cargando...'}
                </Text>
                {turnOrder.length > 0 && (
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    Orden: {turnOrder.join(' → ')}
                  </Text>
                )}
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
              <Title level={4} style={{ margin: 0 }}><TeamOutlined /> {team2.nombre}</Title>
              <Text type="secondary" style={{ fontSize: 12 }}>{team2.jugadores}</Text>
              <div style={{ marginTop: 8 }}>
                <Text strong style={{ fontSize: 24 }}>{team2.puntaje}</Text>
                <Text type="secondary"> puntos</Text>
              </div>
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
