import React from 'react';
import { useGameStore, getUpgradeCost, getProductionPerSecond, type StationId } from '../store/gameStore';
import { ArrowUpCircle, CheckCircle2 } from 'lucide-react';

interface StationProps {
  id: StationId;
}

export const ProductionStation: React.FC<StationProps> = ({ id }) => {
  const { stations, money, upgradeStation } = useGameStore();
  const station = stations[id];
  const cost = getUpgradeCost(station);
  const prodPerSec = getProductionPerSecond(station);
  const canAfford = money >= cost;

  // Calculate progress to next 25-level milestone
  const currentMilestone = Math.floor(station.level / 25) * 25;
  const nextMilestone = currentMilestone + 25;
  const progress = ((station.level - currentMilestone) / 25) * 100;

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-3">
      <div className="flex justify-between items-start mb-3">
        <div>
          <h3 className="font-bold text-gray-800 flex items-center gap-2">
            {station.name}
            {station.level >= 25 && <CheckCircle2 size={16} className="text-green-500" />}
          </h3>
          <div className="text-sm text-gray-500">
            Úroveň {station.level}
          </div>
        </div>
        <div className="text-right">
          <div className="font-bold text-green-600">
            +${prodPerSec.toLocaleString(undefined, { maximumFractionDigits: 1 })}/s
          </div>
        </div>
      </div>

      <div className="mb-3">
        <div className="flex justify-between text-xs text-gray-500 mb-1">
          <span>Bonus x2 pri úrovni {nextMilestone}</span>
          <span>{station.level}/{nextMilestone}</span>
        </div>
        <div className="w-full bg-gray-100 rounded-full h-2">
          <div
            className="bg-blue-500 h-2 rounded-full transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <button
        onClick={() => upgradeStation(id)}
        disabled={!canAfford}
        className={`w-full flex items-center justify-center gap-2 py-2.5 rounded-lg font-medium transition-colors ${
          canAfford
            ? 'bg-blue-600 hover:bg-blue-700 text-white'
            : 'bg-gray-100 text-gray-400 cursor-not-allowed'
        }`}
      >
        <ArrowUpCircle size={18} />
        Vylepšiť (${cost.toLocaleString(undefined, { maximumFractionDigits: 0 })})
      </button>
    </div>
  );
};
