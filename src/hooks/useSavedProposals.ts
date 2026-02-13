import { useState, useEffect, useCallback } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Proposal } from "@/interfaces/solution.interface";

const STORAGE_KEY = "saved_proposals";

export interface SavedProposal {
  id: string;
  proposal: Proposal;
  savedAt: string;
}

const generateId = (proposal: Proposal): string => {
  return `${proposal.departureTime}_${proposal.arrivalTime}_${proposal.legs[0]?.originStopName ?? ""}`;
};

export function useSavedProposals() {
  const [savedProposals, setSavedProposals] = useState<SavedProposal[]>([]);
  const [loading, setLoading] = useState(true);

  const loadSaved = useCallback(async () => {
    try {
      const raw = await AsyncStorage.getItem(STORAGE_KEY);
      if (raw) {
        setSavedProposals(JSON.parse(raw));
      }
    } catch (e) {
      console.error("Failed to load saved proposals:", e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSaved();
  }, [loadSaved]);

  const isSaved = useCallback(
    (proposal: Proposal): boolean => {
      const id = generateId(proposal);
      return savedProposals.some((sp) => sp.id === id);
    },
    [savedProposals],
  );

  const toggleSave = useCallback(
    async (proposal: Proposal) => {
      const id = generateId(proposal);
      let updated: SavedProposal[];

      if (savedProposals.some((sp) => sp.id === id)) {
        updated = savedProposals.filter((sp) => sp.id !== id);
      } else {
        updated = [...savedProposals, { id, proposal, savedAt: new Date().toISOString() }];
      }

      setSavedProposals(updated);
      await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    },
    [savedProposals],
  );

  const removeSaved = useCallback(
    async (id: string) => {
      const updated = savedProposals.filter((sp) => sp.id !== id);
      setSavedProposals(updated);
      await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    },
    [savedProposals],
  );

  const clearAll = useCallback(async () => {
    setSavedProposals([]);
    await AsyncStorage.removeItem(STORAGE_KEY);
  }, []);

  return { savedProposals, loading, isSaved, toggleSave, removeSaved, clearAll, refresh: loadSaved };
}
