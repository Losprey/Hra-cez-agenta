import React from 'react';
import { type StationId } from '../../store/gameStore';
import { ProductionStation } from '../ProductionStation';

const STATION_ORDER: StationId[] = [
  'pvc',
  'cutting',
  'welding',
  'assembly',
  'glass',
  'qc',
  'packaging',
  'expedition'
];

export const ProductionTab: React.FC = () => {
  return (
    <div className="p-4 bg-gray-50 min-h-full">
      <div className="mb-4">
        <h2 className="text-xl font-bold text-gray-800">Výrobná linka</h2>
        <p className="text-sm text-gray-500">Spravuj a vylepšuj svoje stroje</p>
      </div>

      <div className="space-y-4">
        {STATION_ORDER.map(id => (
          <ProductionStation key={id} id={id} />
        ))}
      </div>

      {/* Spacer for bottom nav */}
      <div className="h-20" />
    </div>
  );
};
