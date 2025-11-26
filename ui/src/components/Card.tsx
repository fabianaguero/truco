import React from 'react';
import { Card as AntCard, Typography } from 'antd';
import type { Carta, Palo } from '../types';

const { Text } = Typography;

interface CardProps {
  carta: Carta;
  onClick?: () => void;
  disabled?: boolean;
  selected?: boolean;
  faceDown?: boolean;
}

const suitSymbols: Record<Palo, string> = {
  ESPADA: '🗡️',
  BASTO: '🪵',
  ORO: '🪙',
  COPA: '🏆'
};

const suitColors: Record<Palo, string> = {
  ESPADA: '#1890ff',
  BASTO: '#52c41a',
  ORO: '#faad14',
  COPA: '#f5222d'
};

const suitNames: Record<Palo, string> = {
  ESPADA: 'Espada',
  BASTO: 'Basto',
  ORO: 'Oro',
  COPA: 'Copa'
};

const CardComponent: React.FC<CardProps> = ({ 
  carta, 
  onClick, 
  disabled = false, 
  selected = false,
  faceDown = false 
}) => {
  const cardStyle: React.CSSProperties = {
    width: 80,
    height: 120,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: disabled ? 'not-allowed' : 'pointer',
    border: selected ? '3px solid #1890ff' : '1px solid #d9d9d9',
    borderRadius: 8,
    backgroundColor: faceDown ? '#003366' : '#fff',
    boxShadow: selected ? '0 0 10px rgba(24, 144, 255, 0.5)' : '0 2px 8px rgba(0, 0, 0, 0.15)',
    opacity: disabled ? 0.6 : 1,
    transition: 'all 0.2s ease',
    transform: selected ? 'translateY(-10px)' : 'none'
  };

  if (faceDown) {
    return (
      <AntCard
        style={cardStyle}
        bodyStyle={{ padding: 8, height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
      >
        <div style={{ 
          width: '100%', 
          height: '100%', 
          background: 'repeating-linear-gradient(45deg, #002244, #002244 5px, #003366 5px, #003366 10px)',
          borderRadius: 4
        }} />
      </AntCard>
    );
  }

  const handleClick = () => {
    if (!disabled && onClick) {
      onClick();
    }
  };

  return (
    <AntCard
      hoverable={!disabled}
      onClick={handleClick}
      style={cardStyle}
      bodyStyle={{ padding: 8, height: '100%', width: '100%' }}
    >
      <div style={{ 
        display: 'flex', 
        flexDirection: 'column', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        height: '100%',
        width: '100%'
      }}>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'flex-start', 
          alignItems: 'center',
          width: '100%'
        }}>
          <Text strong style={{ fontSize: 16, color: suitColors[carta.palo] }}>
            {carta.valor}
          </Text>
        </div>
        
        <div style={{ 
          fontSize: 32, 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          flex: 1
        }}>
          {suitSymbols[carta.palo]}
        </div>
        
        <div style={{ 
          display: 'flex', 
          justifyContent: 'flex-end', 
          alignItems: 'center',
          width: '100%'
        }}>
          <Text type="secondary" style={{ fontSize: 10 }}>
            {suitNames[carta.palo]}
          </Text>
        </div>
      </div>
    </AntCard>
  );
};

export default CardComponent;
