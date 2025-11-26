export interface Jugador {
  id: string;
  nombre: string;
  equipos?: Equipo[];
}

export interface Equipo {
  id: string;
  nombre: string;
  jugadores?: Jugador[];
  puntaje: number;
}

// Card types for the game
export type Palo = 'ESPADA' | 'BASTO' | 'ORO' | 'COPA';

export interface Carta {
  id?: number;
  palo: Palo;
  valor: number;
}

export interface Jugada {
  jugadorNombre: string;
  carta: Carta;
  numeroVuelta: number;
  numeroRonda: number;
}

export type EstadoRonda = 'ESPERANDO_JUGADA' | 'TRUCO_CANTADO' | 'ENVIDO_CANTADO' | 'TERMINADA';

export interface PartidaState {
  id: string;
  equipos: Equipo[];
  ronda: number;
  vuelta: number;
  estadoRonda: EstadoRonda;
  trucoCantado: boolean;
  envidoCantado: boolean;
  valorTruco: number;
  valorEnvido: number;
  cartasJugadas: Jugada[];
  turnoActual: string;
}

export interface ManoJugador {
  jugador: string;
  cartas: Carta[];
  turnoActual: string;
  esMiTurno: boolean;
  estadoRonda: EstadoRonda;
  cartasJugadas: Jugada[];
  puntosEquipo1: number;
  puntosEquipo2: number;
}

// WebSocket message types
export interface WebSocketMessage {
  type: 'PARTIDA_UPDATE' | 'CARTA_JUGADA' | 'TRUCO_CANTADO' | 'ENVIDO_CANTADO' | 'TURNO_CAMBIO' | 'QUISO' | 'NO_QUISO' | 'AL_MAZO';
  payload: PartidaState | Jugada | string;
  partidaId: string;
}

export type { Jugador as IJugador, Equipo as IEquipo };