import React from 'react';
import { useGameStore, getTotalProductionPerSecond } from '../../store/gameStore';
import { GENERATE_ORDERS } from '../../store/orders';
import { Package, Building, Home, School, DollarSign, Star, Timer } from 'lucide-react';

export const OrdersTab: React.FC = () => {
  const { stations, activeOrderId, activeOrderProgress, setActiveOrder } = useGameStore();

  const prodPerSec = getTotalProductionPerSecond(stations);

  const orders = GENERATE_ORDERS().map(o => ({
    ...o,
    icon: o.id === '1' ? <Home size={24} className="text-blue-500" /> :
          o.id === '2' ? <Building size={24} className="text-purple-500" /> :
                         <School size={24} className="text-orange-500" />
  }));

  return (
    <div className="p-4 bg-gray-50 min-h-full">
      <div className="mb-4">
        <h2 className="text-xl font-bold text-gray-800">Dostupné Objednávky</h2>
        <p className="text-sm text-gray-500">
          Produkcia celej linky: {Math.floor(prodPerSec)} $/s
        </p>
      </div>

      <div className="space-y-4">
        {orders.map(order => {
          const isActive = activeOrderId === order.id;
          const isBusy = activeOrderId !== null && !isActive;

          return (
            <div key={order.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
              <div className="flex items-center gap-4 mb-4">
                <div className="p-3 bg-gray-50 rounded-lg">
                  {order.icon}
                </div>
                <div className="flex-1">
                  <h3 className="font-bold text-gray-800">{order.type}</h3>
                  <div className="flex items-center gap-1 text-sm text-gray-500">
                    <Package size={14} />
                    <span>{order.windowsRequired} okien</span>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 mb-4">
                <div className="bg-green-50 rounded-lg p-2 flex items-center justify-center gap-1">
                  <DollarSign size={16} className="text-green-600" />
                  <span className="font-bold text-green-700">+{order.rewardMoney.toLocaleString()}</span>
                </div>
                <div className="bg-yellow-50 rounded-lg p-2 flex items-center justify-center gap-1">
                  <Star size={16} className="text-yellow-600" />
                  <span className="font-bold text-yellow-700">+{order.rewardReputation}</span>
                </div>
              </div>

              {isActive ? (
                <div>
                  <div className="flex justify-between text-xs text-gray-500 mb-1">
                    <span className="flex items-center gap-1"><Timer size={12}/> Vybavuje sa...</span>
                    <span>{Math.floor(activeOrderProgress)}%</span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div
                      className="bg-blue-500 h-2 rounded-full transition-all duration-100"
                      style={{ width: `${activeOrderProgress}%` }}
                    />
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => setActiveOrder(order.id)}
                  disabled={isBusy}
                  className={`w-full py-2.5 rounded-lg font-medium transition-colors ${
                    isBusy
                      ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                      : 'bg-blue-50 hover:bg-blue-100 text-blue-700'
                  }`}
                >
                  Prijať Objednávku
                </button>
              )}
            </div>
          );
        })}
      </div>

      {/* Spacer for bottom nav */}
      <div className="h-20" />
    </div>
  );
};
