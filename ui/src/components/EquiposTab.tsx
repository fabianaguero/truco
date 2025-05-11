import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, message, Select } from 'antd';
import type { Equipo, Jugador } from '../types';
import { equipoService, jugadorService } from '../services/api';

const EquiposTab: React.FC = () => {
  const [equipos, setEquipos] = useState<Equipo[]>([]);
  const [jugadores, setJugadores] = useState<Jugador[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const jugadoresValidos = jugadores.filter(
      (j): j is Jugador => j && j.id !== undefined && j.nombre !== undefined
  );

  const columns = [
    {
      title: 'Nombre',
      dataIndex: 'nombre',
      key: 'nombre'
    },
    {
      title: 'Jugadores',
      key: 'jugadores',
      render: (_: any, record: Equipo) => (
          <span>
          {record.jugadores?.map((j, index) => (
              <span key={j.id || `${record.id}-jugador-${index}`}>
              {j.nombre}
                {index < (record.jugadores?.length ?? 0) - 1 ? ', ' : ''}
            </span>
          )) || 'Sin jugadores'}
        </span>
      )
    },
    {
      title: 'Acciones',
      key: 'acciones',
      render: (_: any, record: Equipo) => (
          <div style={{ display: 'flex', gap: '8px' }}>
            <Button type="primary" onClick={() => handleEditar(record)}>
              Editar
            </Button>
            <Button danger onClick={() => handleEliminar(record.id)}>
              Eliminar
            </Button>
          </div>
      )
    }
  ];

  useEffect(() => {
    cargarDatos();
  }, []);

  const cargarDatos = async () => {
    setLoading(true);
    try {
      const [equiposRes, jugadoresRes] = await Promise.all([
        equipoService.listar(),
        jugadorService.listar()
      ]);
      setEquipos(equiposRes.data);
      setJugadores(jugadoresRes.data);
    } catch (error) {
      message.error('Error al cargar los datos');
    } finally {
      setLoading(false);
    }
  };

  const cargarJugadores = async () => {
    try {
      const response = await jugadorService.listar();
      setJugadores(response.data);
    } catch (error) {
      message.error('Error al cargar los jugadores');
    }
  };

  const handleNuevo = async () => {
    await cargarJugadores();
    form.resetFields();
    setModalVisible(true);
  };

  const handleEditar = async (equipo: Equipo) => {
    setLoading(true);
    try {
      const response = await jugadorService.listar();
      setJugadores(response.data);

      const jugadoresFormateados = equipo.jugadores?.map(j => ({
        value: j.id,
        label: j.nombre
      })) || [];

      form.setFieldsValue({
        id: equipo.id,
        nombre: equipo.nombre,
        jugadores: jugadoresFormateados
      });

      setModalVisible(true);
    } catch (error) {
      message.error('Error al cargar jugadores');
    } finally {
      setLoading(false);
    }
  };

  const handleEliminar = async (id: string) => {
    try {
      await equipoService.eliminar(id);
      message.success('Equipo eliminado correctamente');
      await cargarDatos();
    } catch (error) {
      message.error('Error al eliminar el equipo');
    }
  };

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      let equipoId = values.id;

      if (equipoId) {
        await equipoService.actualizar(equipoId, { nombre: values.nombre });
      } else {
        const res = await equipoService.crear({ nombre: values.nombre });
        equipoId = res.data.id;
      }

      // ⚠️ Extraer solo los IDs válidos desde labelInValue
      const jugadoresSeleccionados: string[] = (values.jugadores || [])
          .filter((j: any) => typeof j === 'object' && j.value)
          .map((j: any) => String(j.value));

      console.log("Jugadores a asignar:", jugadoresSeleccionados);

      const equipoActual = equipos.find(e => e.id === equipoId) || { jugadores: [] };
      const jugadoresActuales = equipoActual.jugadores?.map(j => String(j.id)) || [];

      for (const jugadorId of jugadoresSeleccionados) {
        if (!jugadoresActuales.includes(jugadorId)) {
          await equipoService.asignarJugadorAEquipo(equipoId, jugadorId);
        }
      }

      for (const jugadorId of jugadoresActuales) {
        if (!jugadoresSeleccionados.includes(jugadorId)) {
          await equipoService.removerJugadorDeEquipo(equipoId, jugadorId);
        }
      }

      message.success('Equipo guardado correctamente');
      setModalVisible(false);
      form.resetFields();
      await cargarDatos();
    } catch (error) {
      console.error("Error en onFinish:", error);
      message.error('Error al guardar el equipo');
    } finally {
      setLoading(false);
    }
  };

  return (
      <div>
        <Button type="primary" onClick={handleNuevo} style={{ marginBottom: 16 }}>
          Nuevo Equipo
        </Button>

        <Table
            columns={columns}
            dataSource={equipos}
            loading={loading}
            rowKey="id"
        />

        <Modal
            title={form.getFieldValue('id') ? 'Editar Equipo' : 'Nuevo Equipo'}
            open={modalVisible}
            onOk={form.submit}
            onCancel={() => setModalVisible(false)}
            confirmLoading={loading}
        >
          <Form
              form={form}
              onFinish={onFinish}
              layout="vertical"
          >
            <Form.Item name="id" hidden>
              <Input />
            </Form.Item>

            <Form.Item
                name="nombre"
                label="Nombre"
                rules={[{ required: true, message: 'Por favor ingresa el nombre' }]}
            >
              <Input />
            </Form.Item>

            <Form.Item name="jugadores" label="Jugadores">
              <Select
                  mode="multiple"
                  labelInValue
                  showSearch
                  optionFilterProp="children"
                  placeholder="Selecciona jugadores"
              >
                {jugadoresValidos.map(j => (
                    <Select.Option key={j.id} value={j.id}>
                      {j.nombre}
                    </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Form>
        </Modal>
      </div>
  );
};

export default EquiposTab;
