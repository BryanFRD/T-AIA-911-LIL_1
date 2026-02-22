import React from "react";
import { renderHook, act, waitFor } from "@testing-library/react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import {
  SavedProposalsProvider,
  useSavedProposals,
} from "./SavedProposalsContext";
import { Proposal } from "@/interfaces/solution.interface";

const makeLeg = (overrides = {}) => ({
  originStopName: "Gare de Lyon",
  originLat: 48.8448,
  originLon: 2.3735,
  departureTime: "2024-01-15T08:00:00.000Z",
  destinationStopName: "Châtelet",
  destinationLat: 48.8583,
  destinationLon: 2.347,
  arrivalTime: "2024-01-15T08:30:00.000Z",
  routeShortName: "RER A",
  routeLongName: "RER A - Cergy",
  routeColor: "FF0000",
  headSign: "Cergy-le-Haut",
  travelTime: { hours: 0, minutes: 30 },
  ...overrides,
});

const makeProposal = (overrides: Partial<Proposal> = {}): Proposal => ({
  departureTime: "2024-01-15T08:00:00.000Z",
  arrivalTime: "2024-01-15T08:30:00.000Z",
  travelTime: { hours: 0, minutes: 30 },
  legs: [makeLeg()],
  transferCount: 0,
  multiDay: false,
  ...overrides,
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <SavedProposalsProvider>{children}</SavedProposalsProvider>
);

describe("SavedProposalsContext", () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
    jest.clearAllMocks();
  });

  it("lance une erreur quand useSavedProposals est utilisé hors du Provider", () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
    expect(() => {
      renderHook(() => useSavedProposals());
    }).toThrow("useSavedProposals must be used within SavedProposalsProvider");
    consoleSpy.mockRestore();
  });

  it("démarre avec une liste vide", async () => {
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.savedProposals).toHaveLength(0);
  });

  it("charge les propositions depuis AsyncStorage au montage", async () => {
    const proposal = makeProposal();
    const savedItem = {
      id: `${proposal.departureTime}_${proposal.arrivalTime}_${proposal.legs[0].originStopName}`,
      proposal,
      savedAt: new Date().toISOString(),
    };
    await AsyncStorage.setItem("saved_proposals", JSON.stringify([savedItem]));

    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.savedProposals).toHaveLength(1);
    expect(result.current.savedProposals[0].id).toBe(savedItem.id);
  });

  it("toggleSave ajoute une proposition", async () => {
    const proposal = makeProposal();
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.toggleSave(proposal);
    });

    expect(result.current.savedProposals).toHaveLength(1);
    expect(result.current.savedProposals[0].proposal).toEqual(proposal);
  });

  it("toggleSave supprime une proposition déjà sauvegardée", async () => {
    const proposal = makeProposal();
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.toggleSave(proposal);
    });
    expect(result.current.savedProposals).toHaveLength(1);

    await act(async () => {
      await result.current.toggleSave(proposal);
    });
    expect(result.current.savedProposals).toHaveLength(0);
  });

  it("isSaved retourne true pour une proposition sauvegardée", async () => {
    const proposal = makeProposal();
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.isSaved(proposal)).toBe(false);

    await act(async () => {
      await result.current.toggleSave(proposal);
    });

    expect(result.current.isSaved(proposal)).toBe(true);
  });

  it("isSaved retourne false pour une proposition non sauvegardée", async () => {
    const proposal = makeProposal();
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.isSaved(proposal)).toBe(false);
  });

  it("removeSaved supprime une proposition spécifique par id", async () => {
    const proposal1 = makeProposal({ departureTime: "2024-01-15T08:00:00.000Z" });
    const proposal2 = makeProposal({ departureTime: "2024-01-15T09:00:00.000Z" });
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.toggleSave(proposal1);
    });
    await act(async () => {
      await result.current.toggleSave(proposal2);
    });
    expect(result.current.savedProposals).toHaveLength(2);

    const idToRemove = result.current.savedProposals[0].id;
    await act(async () => {
      await result.current.removeSaved(idToRemove);
    });

    expect(result.current.savedProposals).toHaveLength(1);
    expect(result.current.savedProposals[0].id).not.toBe(idToRemove);
  });

  it("clearAll vide toutes les propositions", async () => {
    const proposal = makeProposal();
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.toggleSave(proposal);
    });
    expect(result.current.savedProposals).toHaveLength(1);

    await act(async () => {
      await result.current.clearAll();
    });

    expect(result.current.savedProposals).toHaveLength(0);
  });

  it("clearAll supprime également la clé AsyncStorage", async () => {
    const proposal = makeProposal();
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.toggleSave(proposal);
    });
    await act(async () => {
      await result.current.clearAll();
    });

    const stored = await AsyncStorage.getItem("saved_proposals");
    expect(stored).toBeNull();
  });

  it("refresh recharge les données depuis AsyncStorage", async () => {
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.savedProposals).toHaveLength(0);

    // Ajouter directement dans AsyncStorage sans passer par le hook
    const proposal = makeProposal();
    const savedItem = {
      id: `${proposal.departureTime}_${proposal.arrivalTime}_${proposal.legs[0].originStopName}`,
      proposal,
      savedAt: new Date().toISOString(),
    };
    await AsyncStorage.setItem("saved_proposals", JSON.stringify([savedItem]));

    await act(async () => {
      await result.current.refresh();
    });

    expect(result.current.savedProposals).toHaveLength(1);
  });

  it("persiste les données dans AsyncStorage après toggleSave", async () => {
    const proposal = makeProposal();
    const { result } = renderHook(() => useSavedProposals(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.toggleSave(proposal);
    });

    const stored = await AsyncStorage.getItem("saved_proposals");
    expect(stored).not.toBeNull();
    const parsed = JSON.parse(stored!);
    expect(parsed).toHaveLength(1);
    expect(parsed[0].proposal).toEqual(proposal);
  });
});
