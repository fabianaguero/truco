import axios from 'axios';
import type { IJugador as Jugador, IEquipo as Equipo } from '../types';

const axiosInstance = axios.create({
    baseURL: '/api',
    headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    },
    timeout: 5000,
    withCredentials: true
});

axiosInstance.interceptors.request.use(
    config => {
        console.log('Enviando petición:', {
            method: config.method?.toUpperCase(),
            url: config.url,
            baseURL: config.baseURL,
            headers: config.headers,
            data: config.data
        });
        return config;
    },
    error => {
        console.error('Error en petición:', error);
        return Promise.reject(error);
    }
);

axiosInstance.interceptors.response.use(
    response => {
        console.log('Respuesta recibida:', {
            status: response.status,
            url: response.config.url,
            data: response.data
        });
        return response;
    },
    error => {
        if (error.response) {
            console.error('Error de respuesta:', {
                status: error.response.status,
                data: error.response.data,
                headers: error.response.headers
            });
        } else if (error.request) {
            console.error('Error de red:', error.request);
        } else {
            console.error('Error de configuración:', error.message);
        }
        return Promise.reject(error);
    }
);

export const jugadorService = {
    listar: () => axiosInstance.get<Jugador[]>('/jugadores'),
    crear: (jugador: { nombre: string, equipoIds?: string[] }) =>
        axiosInstance.post<Jugador>('/jugadores', jugador),
    actualizar: (id: string, jugador: { nombre: string, equipoIds?: string[] }) =>
        axiosInstance.put<Jugador>(`/jugadores/${id}`, jugador),
    eliminar: (id: string) =>
        axiosInstance.delete(`/jugadores/${id}`)
};

export const equipoService = {
    listar: () => axiosInstance.get<Equipo[]>('/equipos'),
    crear: (equipo: { nombre: string }) =>
        axiosInstance.post<Equipo>('/equipos', equipo),
    actualizar: (id: string, equipo: { nombre: string }) =>
        axiosInstance.put<Equipo>(`/equipos/${id}`, equipo),
    eliminar: (id: string) =>
        axiosInstance.delete(`/equipos/${id}`),
    asignarJugadorAEquipo: (equipoId: string, jugadorId: string) =>
        axiosInstance.post(`/equipos/${equipoId}/jugadores/${jugadorId}`),
    removerJugadorDeEquipo: (equipoId: string, jugadorId: string) =>
        axiosInstance.delete(`/equipos/${equipoId}/jugadores/${jugadorId}`)
};