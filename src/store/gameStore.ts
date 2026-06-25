import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type StationId =
  | 'pvc'
  | 'cutting'
  | 'welding'
  | 'assembly'
  | 'glass'
  | 'qc'
  | 'packaging'
  | 'expedition';

export interface Station {
  id: StationId;
  name: string;
  level: number;
  baseProduction: number;
  baseUpgradeCost: number;
  costMultiplier: number;
}

export interface GameState {
  money: number;
  diamonds: number;
  reputation: number;
  ordersCompleted: number;
  stations: Record<StationId, Station>;
  lastSaved: number;
  activeOrderId: string | null;
  activeOrderProgress: number; // 0 to 100

  // Actions
  addMoney: (amount: number) => void;
  upgradeStation: (id: StationId) => void;
  setActiveOrder: (id: string | null) => void;
  updateOrderProgress: (progressAmount: number) => void;
  completeOrder: (rewardMoney: number, rewardReputation: number) => void;
  updateOfflineProgress: (currentTime: number) => void;
}

const INITIAL_STATIONS: Record<StationId, Station> = {
  pvc: { id: 'pvc', name: 'PVC Profil', level: 1, baseProduction: 1, baseUpgradeCost: 10, costMultiplier: 1.15 },
  cutting: { id: 'cutting', name: 'Rezanie', level: 0, baseProduction: 5, baseUpgradeCost: 50, costMultiplier: 1.15 },
  welding: { id: 'welding', name: 'Zváranie rámu', level: 0, baseProduction: 25, baseUpgradeCost: 250, costMultiplier: 1.15 },
  assembly: { id: 'assembly', name: 'Montáž kovania', level: 0, baseProduction: 100, baseUpgradeCost: 1000, costMultiplier: 1.15 },
  glass: { id: 'glass', name: 'Vloženie skla', level: 0, baseProduction: 400, baseUpgradeCost: 4000, costMultiplier: 1.15 },
  qc: { id: 'qc', name: 'Kontrola kvality', level: 0, baseProduction: 1500, baseUpgradeCost: 15000, costMultiplier: 1.15 },
  packaging: { id: 'packaging', name: 'Balenie', level: 0, baseProduction: 6000, baseUpgradeCost: 60000, costMultiplier: 1.15 },
  expedition: { id: 'expedition', name: 'Expedícia', level: 0, baseProduction: 25000, baseUpgradeCost: 250000, costMultiplier: 1.15 },
};

export const getUpgradeCost = (station: Station) => {
  return Math.floor(station.baseUpgradeCost * Math.pow(station.costMultiplier, station.level));
};

export const getProductionPerSecond = (station: Station) => {
  if (station.level === 0) return 0;
  // Apply 25-level bonuses
  const bonusMultiplier = Math.pow(2, Math.floor(station.level / 25));
  return station.baseProduction * station.level * bonusMultiplier;
};

export const getTotalProductionPerSecond = (stations: Record<StationId, Station>) => {
  return Object.values(stations).reduce((total, station) => {
    return total + getProductionPerSecond(station);
  }, 0);
};

export const useGameStore = create<GameState>()(
  persist(
    (set) => ({
      money: 0,
      diamonds: 0,
      reputation: 0,
      ordersCompleted: 0,
      stations: INITIAL_STATIONS,
      lastSaved: Date.now(),
      activeOrderId: null,
      activeOrderProgress: 0,

      addMoney: (amount: number) => set((state) => ({ money: state.money + amount })),

      setActiveOrder: (id: string | null) => set({ activeOrderId: id, activeOrderProgress: 0 }),

      updateOrderProgress: (progressAmount: number) => set((state) => ({
        activeOrderProgress: Math.min(100, state.activeOrderProgress + progressAmount)
      })),

      upgradeStation: (id: StationId) => {
        set((state) => {
          const station = state.stations[id];
          const cost = getUpgradeCost(station);

          if (state.money >= cost) {
            return {
              money: state.money - cost,
              stations: {
                ...state.stations,
                [id]: {
                  ...station,
                  level: station.level + 1
                }
              }
            };
          }
          return state;
        });
      },

      completeOrder: (rewardMoney: number, rewardReputation: number) => {
        set((state) => ({
          money: state.money + rewardMoney,
          reputation: state.reputation + rewardReputation,
          ordersCompleted: state.ordersCompleted + 1,
          activeOrderId: null,
          activeOrderProgress: 0,
        }));
      },

      updateOfflineProgress: (currentTime: number) => {
        set((state) => {
          const timeDiffSeconds = Math.floor((currentTime - state.lastSaved) / 1000);

          if (timeDiffSeconds > 0) {
            const prodPerSec = getTotalProductionPerSecond(state.stations);
            const earned = prodPerSec * timeDiffSeconds;

            return {
              money: state.money + earned,
              lastSaved: currentTime
            };
          }
          return state;
        });
      }
    }),
    {
      name: 'window-empire-storage',
      // Provide custom storage implementation to catch errors safely
      storage: {
        getItem: (name) => {
          try {
            const str = localStorage.getItem(name);
            return str ? JSON.parse(str) : null;
          } catch (e) {
            console.warn('localStorage is not available', e);
            return null;
          }
        },
        setItem: (name, value) => {
          try {
            localStorage.setItem(name, JSON.stringify(value));
          } catch (e) {
            console.warn('localStorage is not available', e);
          }
        },
        removeItem: (name) => {
          try {
            localStorage.removeItem(name);
          } catch (e) {
            console.warn('localStorage is not available', e);
          }
        },
      },
    }
  )
);
