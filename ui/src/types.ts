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

export type { Jugador as IJugador, Equipo as IEquipo };