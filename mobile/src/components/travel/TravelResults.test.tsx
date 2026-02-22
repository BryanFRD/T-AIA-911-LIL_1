import React from "react";
import { render, screen, fireEvent } from "@testing-library/react-native";
import TravelResults from "./TravelResults";
import { SolutionResponse } from "@/interfaces/solution.interface";

jest.mock("lucide-react-native", () =>
  new Proxy({}, { get: () => () => null })
);

jest.mock("@/hooks/useSavedProposals", () => ({
  useSavedProposals: () => ({
    isSaved: jest.fn().mockReturnValue(false),
    toggleSave: jest.fn(),
    savedProposals: [],
    loading: false,
    removeSaved: jest.fn(),
    clearAll: jest.fn(),
    refresh: jest.fn(),
  }),
}));

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

const makeProposal = (overrides = {}) => ({
  departureTime: "2024-01-15T08:00:00.000Z",
  arrivalTime: "2024-01-15T08:30:00.000Z",
  travelTime: { hours: 0, minutes: 30 },
  legs: [makeLeg()],
  transferCount: 0,
  multiDay: false,
  ...overrides,
});

const makeSolution = (overrides: Partial<SolutionResponse> = {}): SolutionResponse => ({
  customerQuery: {
    origin: "Gare de Lyon",
    destination: "Châtelet",
    vias: [],
    departureTime: "2024-01-15T08:00:00.000Z",
  },
  proposals: [makeProposal()],
  ...overrides,
});

const defaultProps = {
  solution: makeSolution(),
  onSave: jest.fn(),
  onNewRequest: jest.fn(),
};

describe("TravelResults", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("affiche l'origine et la destination", () => {
    render(<TravelResults {...defaultProps} />);
    expect(screen.getByText("Gare de Lyon")).toBeTruthy();
    expect(screen.getByText("Châtelet")).toBeTruthy();
  });

  it("affiche 'Itinéraire trouvé'", () => {
    render(<TravelResults {...defaultProps} />);
    expect(screen.getByText("Itinéraire trouvé")).toBeTruthy();
  });

  it("affiche '1 proposition' pour une seule proposition", () => {
    render(<TravelResults {...defaultProps} />);
    expect(screen.getByText("1 proposition")).toBeTruthy();
  });

  it("affiche '2 propositions' pour deux propositions", () => {
    const solution = makeSolution({ proposals: [makeProposal(), makeProposal()] });
    render(<TravelResults {...defaultProps} solution={solution} />);
    expect(screen.getByText("2 propositions")).toBeTruthy();
  });

  it("affiche le bouton 'Nouvelle recherche'", () => {
    render(<TravelResults {...defaultProps} />);
    expect(screen.getByText("Nouvelle recherche")).toBeTruthy();
  });

  it("appelle onNewRequest quand 'Nouvelle recherche' est pressé", () => {
    const onNewRequest = jest.fn();
    render(<TravelResults {...defaultProps} onNewRequest={onNewRequest} />);
    fireEvent.press(screen.getByText("Nouvelle recherche"));
    expect(onNewRequest).toHaveBeenCalledTimes(1);
  });

  it("affiche les vias quand présents", () => {
    const solution = makeSolution({
      customerQuery: {
        origin: "Paris",
        destination: "Marseille",
        vias: ["Lyon"],
        departureTime: "2024-01-15T08:00:00.000Z",
      },
    });
    render(<TravelResults {...defaultProps} solution={solution} />);
    expect(screen.getByText("Lyon")).toBeTruthy();
  });

  it("affiche le ProposalDetail quand une proposition est sélectionnée", () => {
    render(<TravelResults {...defaultProps} />);
    // Appuyer sur le label "Départ" de la carte pour ouvrir le détail
    fireEvent.press(screen.getByText("Départ"));
    expect(screen.getByText("Détail du trajet")).toBeTruthy();
  });

  it("revient à la liste quand on appuie sur retour dans le détail", () => {
    render(<TravelResults {...defaultProps} />);
    fireEvent.press(screen.getByText("Départ"));
    expect(screen.getByText("Détail du trajet")).toBeTruthy();

    const backButton = screen.UNSAFE_getAllByType(
      require("react-native").TouchableOpacity
    )[0];
    fireEvent.press(backButton);
    expect(screen.queryByText("Détail du trajet")).toBeNull();
    expect(screen.getByText("Itinéraire trouvé")).toBeTruthy();
  });
});
