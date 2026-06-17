import React from 'react';
import { Factory, ClipboardList, FlaskConical, RotateCcw, ShoppingCart } from 'lucide-react';

export type TabId = 'production' | 'orders' | 'research' | 'prestige' | 'shop';

interface BottomNavProps {
  activeTab: TabId;
  setActiveTab: (tab: TabId) => void;
}

export const BottomNav: React.FC<BottomNavProps> = ({ activeTab, setActiveTab }) => {
  const tabs = [
    { id: 'production', icon: Factory, label: 'Výroba' },
    { id: 'orders', icon: ClipboardList, label: 'Objednávky' },
    { id: 'research', icon: FlaskConical, label: 'Výskum' },
    { id: 'prestige', icon: RotateCcw, label: 'Prestíž' },
    { id: 'shop', icon: ShoppingCart, label: 'Obchod' },
  ] as const;

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 pb-safe">
      <div className="flex justify-around items-center h-16 max-w-md mx-auto">
        {tabs.map(({ id, icon: Icon, label }) => {
          const isActive = activeTab === id;
          return (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              className={`flex flex-col items-center justify-center w-full h-full space-y-1 transition-colors ${
                isActive ? 'text-blue-600' : 'text-gray-500 hover:text-gray-900'
              }`}
            >
              <Icon size={24} className={isActive ? 'stroke-[2.5px]' : ''} />
              <span className="text-[10px] font-medium">{label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
};
