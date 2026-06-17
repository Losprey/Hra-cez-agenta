import { useEffect, useRef } from 'react';
import { useGameStore, getTotalProductionPerSecond } from '../store/gameStore';
import { GENERATE_ORDERS } from '../store/orders';

export const useGameLoop = () => {
  const {
    updateOfflineProgress,
    stations,
    addMoney,
    activeOrderId,
    updateOrderProgress,
    completeOrder
  } = useGameStore();
  const lastTickRef = useRef<number | null>(null);

  // Initialize lastTickRef safely on first load
  useEffect(() => {
    lastTickRef.current = Date.now();
  }, []);

  // Handle offline progress on initial load
  useEffect(() => {
    updateOfflineProgress(Date.now());
  }, [updateOfflineProgress]);

  // Main game loop
  useEffect(() => {
    const tickRate = 1000; // 1 second per tick

    const interval = setInterval(() => {
      if (lastTickRef.current === null) return;

      const now = Date.now();
      const delta = now - lastTickRef.current;

      // Calculate production based on time elapsed since last tick
      // This handles cases where setInterval might be delayed (e.g. background tab)
      if (delta >= tickRate) {
        const secondsElapsed = delta / 1000;
        const prodPerSec = getTotalProductionPerSecond(stations);

        if (prodPerSec > 0) {
          addMoney(prodPerSec * secondsElapsed);
        }

        // Handle global order progress timer
        if (activeOrderId) {
          const orders = GENERATE_ORDERS();
          const activeOrder = orders.find(o => o.id === activeOrderId);
          if (activeOrder) {
            // How much percentage was completed this tick
            const progressAdded = (secondsElapsed / activeOrder.durationSeconds) * 100;
            updateOrderProgress(progressAdded);

            // Note: In a complete implementation, the completion might be handled
            // here by checking if state.activeOrderProgress >= 100,
            // but we need the latest state. We'll handle it via another effect to avoid stale closures.
          }
        }

        lastTickRef.current = now;
      }
    }, 100); // Check frequently, but only process when a full second has elapsed

    return () => clearInterval(interval);
  }, [stations, addMoney, activeOrderId, updateOrderProgress]);

  // Effect to handle order completion when progress reaches 100
  const activeOrderProgress = useGameStore(state => state.activeOrderProgress);
  useEffect(() => {
    if (activeOrderId && activeOrderProgress >= 100) {
      const orders = GENERATE_ORDERS();
      const activeOrder = orders.find(o => o.id === activeOrderId);
      if (activeOrder) {
        completeOrder(activeOrder.rewardMoney, activeOrder.rewardReputation);
      }
    }
  }, [activeOrderId, activeOrderProgress, completeOrder]);
};
