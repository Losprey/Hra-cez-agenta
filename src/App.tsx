import { useState } from 'react';
import { useGameLoop } from './hooks/useGameLoop';
import { BottomNav, type TabId } from './components/BottomNav';

import { ProductionTab } from './components/tabs/ProductionTab';
import { OrdersTab } from './components/tabs/OrdersTab';
import { Header } from './components/Header';

// Placeholders for tabs (will be created in next steps)
const ResearchTab = () => <div className="p-4 flex h-full items-center justify-center text-gray-500 font-medium">Výskum (Čoskoro)</div>;
const PrestigeTab = () => <div className="p-4 flex h-full items-center justify-center text-gray-500 font-medium">Prestíž (Čoskoro)</div>;
const ShopTab = () => <div className="p-4 flex h-full items-center justify-center text-gray-500 font-medium">Obchod (Čoskoro)</div>;

function App() {
  const [activeTab, setActiveTab] = useState<TabId>('production');

  // Start game loop
  useGameLoop();

  const renderTab = () => {
    switch (activeTab) {
      case 'production': return <ProductionTab />;
      case 'orders': return <OrdersTab />;
      case 'research': return <ResearchTab />;
      case 'prestige': return <PrestigeTab />;
      case 'shop': return <ShopTab />;
      default: return <ProductionTab />;
    }
  };

  return (
    <div className="flex justify-center min-h-screen bg-gray-100">
      {/* Mobile container constraint */}
      <div className="w-full max-w-md bg-white min-h-screen flex flex-col relative pb-16">
        <Header />

        {/* Main Content Area */}
        <main className="flex-1 overflow-y-auto">
          {renderTab()}
        </main>

        <BottomNav activeTab={activeTab} setActiveTab={setActiveTab} />
      </div>
    </div>
  );
}

export default App;
