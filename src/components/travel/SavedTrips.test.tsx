import React from "react";
import { Alert } from "react-native";
import { render, screen, fireEvent, waitFor } from "@testing-library/react-native";
import SavedTrips from "./SavedTrips";

jest.mock("lucide-react-native", () =>
  new Proxy({}, { get: () => () => null })
);

const mockRemoveSaved = jest.fn();
const mockToggleSave = jest.fn();
const mockRefresh = jest.fn().mockResolvedValue(undefined);
const mockIsSaved = jest.fn().mockReturnValue(false);

const makeSavedProposal = (id: string, overrides = {}) => ({
  id,
  savedAt: "2024-01-15T10:00:00.000Z",
  proposal: {
    departureTime: "2024-01-15T08:00:00.000Z",
    arrivalTime: "2024-01-15T08:30:00.000Z",
    travelTime: { hours: 0, minutes: 30 },
    legs: [
      {
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
      },
    ],
    transferCount: 0,
    multiDay: false,
    ...overrides,
  },
});

const mockUseSavedProposals = jest.fn();

jest.mock("@/hooks/useSavedProposals", () => ({
  useSavedProposals: (...args: unknown[]) => mockUseSavedProposals(...args),
}));

describe("SavedTrips", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [],
      loading: false,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
  });

  it("affiche le titre 'Trajets sauvegardés'", () => {
    render(<SavedTrips />);
    expect(screen.getByText("Trajets sauvegardés")).toBeTruthy();
  });

  it("affiche l'état de chargement", () => {
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [],
      loading: true,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
    render(<SavedTrips />);
    expect(screen.getByText("Chargement…")).toBeTruthy();
  });

  it("affiche l'état vide quand aucun trajet sauvegardé", () => {
    render(<SavedTrips />);
    expect(screen.getByText("Aucun trajet sauvegardé")).toBeTruthy();
    expect(
      screen.getByText(
        "Sauvegardez des propositions de trajet pour les retrouver ici."
      )
    ).toBeTruthy();
  });

  it("affiche les trajets sauvegardés", () => {
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [makeSavedProposal("id-1")],
      loading: false,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
    render(<SavedTrips />);
    expect(screen.getByText("Gare de Lyon")).toBeTruthy();
    expect(screen.getByText("Châtelet")).toBeTruthy();
  });

  it("affiche la durée du trajet", () => {
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [makeSavedProposal("id-1")],
      loading: false,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
    render(<SavedTrips />);
    expect(screen.getByText("30 min")).toBeTruthy();
  });

  it("affiche plusieurs trajets sauvegardés", () => {
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [
        makeSavedProposal("id-1"),
        makeSavedProposal("id-2"),
      ],
      loading: false,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
    render(<SavedTrips />);
    expect(screen.getAllByText("Gare de Lyon")).toHaveLength(2);
  });

  it("affiche une alerte de confirmation quand le bouton supprimer est pressé", () => {
    const alertSpy = jest.spyOn(Alert, "alert");
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [makeSavedProposal("id-1")],
      loading: false,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
    render(<SavedTrips />);

    // Trouver et presser le bouton de suppression (TouchableOpacity avec hitSlop)
    const deleteButtons = screen.UNSAFE_getAllByProps({ hitSlop: 8 });
    fireEvent.press(deleteButtons[0]);
    expect(alertSpy).toHaveBeenCalledWith(
      "Supprimer",
      "Voulez-vous supprimer ce trajet sauvegardé ?",
      expect.any(Array)
    );
  });

  it("affiche le ProposalDetail quand un trajet est pressé", () => {
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [makeSavedProposal("id-1")],
      loading: false,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
    render(<SavedTrips />);
    fireEvent.press(screen.getByText("Gare de Lyon"));
    expect(screen.getByText("Détail du trajet")).toBeTruthy();
  });

  it("appelle refresh quand le pull-to-refresh est déclenché", async () => {
    mockUseSavedProposals.mockReturnValue({
      savedProposals: [makeSavedProposal("id-1")],
      loading: false,
      removeSaved: mockRemoveSaved,
      isSaved: mockIsSaved,
      toggleSave: mockToggleSave,
      refresh: mockRefresh,
    });
    render(<SavedTrips />);
    const refreshControl = screen.UNSAFE_getByType(
      require("react-native").RefreshControl
    );
    fireEvent(refreshControl, "refresh");
    await waitFor(() => {
      expect(mockRefresh).toHaveBeenCalled();
    });
  });
});
