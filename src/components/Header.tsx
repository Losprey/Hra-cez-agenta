import React from 'react';
import { useGameStore, getTotalProductionPerSecond } from '../store/gameStore';
import { DollarSign, Gem, Star, Package } from 'lucide-react';

export const Header: React.FC = () => {
  const { money, diamonds, reputation, ordersCompleted, stations } = useGameStore();
  const prodPerSec = getTotalProductionPerSecond(stations);

  return (
    <header className="bg-blue-600 text-white p-4 sticky top-0 z-10 shadow-md">
      <div className="flex justify-between items-center mb-3">
        <h1 className="text-xl font-black tracking-tight">WINDOW EMPIRE</h1>
        <div className="flex items-center gap-1 bg-blue-700/50 px-2 py-1 rounded-md text-sm">
          <Package size={14} />
          <span>{ordersCompleted}</span>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-2">
        <div className="bg-white/10 rounded-lg p-2 flex flex-col items-center justify-center">
          <div className="flex items-center gap-1 text-green-300 mb-1">
            <DollarSign size={16} />
            <span className="font-bold text-lg leading-none">
              {Math.floor(money).toLocaleString()}
            </span>
          </div>
          <span className="text-[10px] text-blue-100 uppercase font-medium tracking-wider">
            +${Math.floor(prodPerSec)}/s
          </span>
        </div>

        <div className="bg-white/10 rounded-lg p-2 flex flex-col items-center justify-center">
          <div className="flex items-center gap-1 text-blue-200 mb-1">
            <Gem size={16} className="fill-blue-300" />
            <span className="font-bold text-lg leading-none">
              {diamonds.toLocaleString()}
            </span>
          </div>
          <span className="text-[10px] text-blue-100 uppercase font-medium tracking-wider">
            Diamanty
          </span>
        </div>

        <div className="bg-white/10 rounded-lg p-2 flex flex-col items-center justify-center">
          <div className="flex items-center gap-1 text-yellow-300 mb-1">
            <Star size={16} className="fill-yellow-300" />
            <span className="font-bold text-lg leading-none">
              {reputation.toLocaleString()}
            </span>
          </div>
          <span className="text-[10px] text-blue-100 uppercase font-medium tracking-wider">
            Reputácia
          </span>
        </div>
      </div>
    </header>
  );
};
