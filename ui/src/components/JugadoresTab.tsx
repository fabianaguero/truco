import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, message } from 'antd';
import type { Jugador } from '../types';
import { jugadorService } from '../services/api';

const JugadoresTab: React.FC = () => {
  const [jugadores, setJugadores] = useState<Jugador[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const columns = [
    {
      title: 'UUID',
      dataIndex: 'id',
      key: 'id'
    },
    {
      title: 'Nombre',
      dataIndex: 'nombre',
      key: 'nombre'
    },
    {
      title: 'Acciones',
      key: 'acciones',
      render: (_: any, record: Jugador) => (
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
    cargarJugadores();
  }, []);

  const cargarJugadores = async () => {
    setLoading(true);
    try {
      const response = await jugadorService.listar();
      setJugadores(response.data);
    } catch (error) {
      message.error('Error al cargar los jugadores');
    } finally {
      setLoading(false);
    }
  };

  const handleNuevo = () => {
    form.resetFields();
    setModalVisible(true);
  };

  const handleEditar = (jugador: Jugador) => {
    form.setFieldsValue({
      id: jugador.id,
      nombre: jugador.nombre
    });
    setModalVisible(true);
  };

  const handleEliminar = async (id: string) => {
    try {
      await jugadorService.eliminar(id);
      message.success('Jugador eliminado correctamente');
      await cargarJugadores();
    } catch (error) {
      message.error('Error al eliminar el jugador');
    }
  };

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      if (values.id) {
        await jugadorService.actualizar(values.id, { nombre: values.nombre });
      } else {
        await jugadorService.crear({ nombre: values.nombre });
      }
      message.success('Jugador guardado correctamente');
      setModalVisible(false);
      form.resetFields();
      await cargarJugadores();
    } catch (error) {
      message.error('Error al guardar el jugador');
    } finally {
      setLoading(false);
    }
  };

  return (
      <div>
        <Button type="primary" onClick={handleNuevo} style={{ marginBottom: 16 }}>
          Nuevo Jugador
        </Button>

        <Table
            columns={columns}
            dataSource={jugadores}
            loading={loading}
            rowKey="id"
        />

        <Modal
            title={form.getFieldValue('id') ? 'Editar Jugador' : 'Nuevo Jugador'}
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
          </Form>
        </Modal>
      </div>
  );
};

export default JugadoresTab;