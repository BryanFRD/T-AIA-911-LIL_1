import React from "react";
import { render, screen, fireEvent } from "@testing-library/react-native";
import ProposalDetail from "./ProposalDetail";
import { Proposal } from "@/interfaces/solution.interface";

jest.mock("lucide-react-native", () =>
  new Proxy({}, { get: () => () => null })
);

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

const defaultProps = {
  proposal: makeProposal(),
  onBack: jest.fn(),
  isSaved: false,
  onToggleSave: jest.fn(),
};

describe("ProposalDetail", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("affiche le titre 'Détail du trajet'", () => {
    render(<ProposalDetail {...defaultProps} />);
    expect(screen.getByText("Détail du trajet")).toBeTruthy();
  });

  it("affiche la durée du trajet", () => {
    render(<ProposalDetail {...defaultProps} />);
    expect(screen.getAllByText("30 min").length).toBeGreaterThan(0);
  });

  it("affiche la durée en heures et minutes", () => {
    const proposal = makeProposal({ travelTime: { hours: 1, minutes: 15 } });
    render(<ProposalDetail {...defaultProps} proposal={proposal} />);
    expect(screen.getByText("1h15")).toBeTruthy();
  });

  it("appelle onBack quand le bouton retour est pressé", () => {
    const onBack = jest.fn();
    render(<ProposalDetail {...defaultProps} onBack={onBack} />);
    const backButton = screen.UNSAFE_getAllByType(
      require("react-native").TouchableOpacity
    )[0];
    fireEvent.press(backButton);
    expect(onBack).toHaveBeenCalledTimes(1);
  });

  it("affiche 'Sauvegarder' quand isSaved = false", () => {
    render(<ProposalDetail {...defaultProps} isSaved={false} />);
    expect(screen.getByText("Sauvegarder")).toBeTruthy();
  });

  it("affiche 'Supprimer' quand isSaved = true", () => {
    render(<ProposalDetail {...defaultProps} isSaved={true} />);
    expect(screen.getByText("Supprimer")).toBeTruthy();
  });

  it("appelle onToggleSave quand le bouton save est pressé", () => {
    const onToggleSave = jest.fn();
    render(<ProposalDetail {...defaultProps} onToggleSave={onToggleSave} />);
    fireEvent.press(screen.getByText("Sauvegarder"));
    expect(onToggleSave).toHaveBeenCalledTimes(1);
  });

  it("affiche les noms des arrêts dans la timeline", () => {
    render(<ProposalDetail {...defaultProps} />);
    expect(screen.getAllByText("Gare de Lyon").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Châtelet").length).toBeGreaterThan(0);
  });

  it("affiche le nom de la ligne", () => {
    render(<ProposalDetail {...defaultProps} />);
    expect(screen.getByText("RER A")).toBeTruthy();
  });

  it("affiche la direction quand headSign est défini", () => {
    render(<ProposalDetail {...defaultProps} />);
    expect(screen.getByText("Direction Cergy-le-Haut")).toBeTruthy();
  });

  it("affiche le nombre de correspondances quand > 0", () => {
    const proposal = makeProposal({ transferCount: 2 });
    render(<ProposalDetail {...defaultProps} proposal={proposal} />);
    expect(screen.getByText("2 correspondances")).toBeTruthy();
  });

  it("affiche 'correspondance' au singulier pour 1 transfert", () => {
    const proposal = makeProposal({ transferCount: 1 });
    render(<ProposalDetail {...defaultProps} proposal={proposal} />);
    expect(screen.getByText("1 correspondance")).toBeTruthy();
  });

  it("affiche 'Multi-jour' quand multiDay = true", () => {
    const proposal = makeProposal({ multiDay: true });
    render(<ProposalDetail {...defaultProps} proposal={proposal} />);
    expect(screen.getByText("Multi-jour")).toBeTruthy();
  });

  it("n'affiche pas 'Multi-jour' quand multiDay = false", () => {
    render(<ProposalDetail {...defaultProps} />);
    expect(screen.queryByText("Multi-jour")).toBeNull();
  });

  it("affiche le badge 'Correspondance' entre deux legs", () => {
    const proposal = makeProposal({
      legs: [makeLeg(), makeLeg({ routeShortName: "M14" })],
    });
    render(<ProposalDetail {...defaultProps} proposal={proposal} />);
    expect(screen.getByText("Correspondance")).toBeTruthy();
  });
});
