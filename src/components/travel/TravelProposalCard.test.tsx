import React from "react";
import { render, screen, fireEvent } from "@testing-library/react-native";
import TravelProposalCard from "./TravelProposalCard";
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

describe("TravelProposalCard", () => {
  const onPress = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("affiche les labels Départ et Arrivée", () => {
    render(<TravelProposalCard proposal={makeProposal()} index={0} onPress={onPress} />);
    expect(screen.getByText("Départ")).toBeTruthy();
    expect(screen.getByText("Arrivée")).toBeTruthy();
  });

  it("affiche le nom de la ligne", () => {
    render(<TravelProposalCard proposal={makeProposal()} index={0} onPress={onPress} />);
    expect(screen.getByText("RER A")).toBeTruthy();
  });

  it("affiche le nombre d'étapes", () => {
    render(<TravelProposalCard proposal={makeProposal()} index={0} onPress={onPress} />);
    expect(screen.getByText(" 1 étapes")).toBeTruthy();
  });

  it("affiche le nombre d'étapes pour un trajet multi-legs", () => {
    const proposal = makeProposal({
      legs: [makeLeg(), makeLeg({ routeShortName: "M14" })],
    });
    render(<TravelProposalCard proposal={proposal} index={0} onPress={onPress} />);
    expect(screen.getByText(" 2 étapes")).toBeTruthy();
  });

  it("affiche la durée du trajet (minutes)", () => {
    render(<TravelProposalCard proposal={makeProposal()} index={0} onPress={onPress} />);
    expect(screen.getByText("30 min")).toBeTruthy();
  });

  it("affiche la durée en heures et minutes", () => {
    const proposal = makeProposal({ travelTime: { hours: 1, minutes: 25 } });
    render(<TravelProposalCard proposal={proposal} index={0} onPress={onPress} />);
    expect(screen.getByText("1h25")).toBeTruthy();
  });

  it("affiche la durée en heures seulement", () => {
    const proposal = makeProposal({ travelTime: { hours: 2, minutes: 0 } });
    render(<TravelProposalCard proposal={proposal} index={0} onPress={onPress} />);
    expect(screen.getByText("2h")).toBeTruthy();
  });

  it("affiche le nombre de correspondances quand > 0", () => {
    const proposal = makeProposal({ transferCount: 2 });
    render(<TravelProposalCard proposal={proposal} index={0} onPress={onPress} />);
    expect(screen.getByText("2 corresp.")).toBeTruthy();
  });

  it("n'affiche pas les correspondances quand transferCount = 0", () => {
    render(<TravelProposalCard proposal={makeProposal()} index={0} onPress={onPress} />);
    expect(screen.queryByText(/corresp./)).toBeNull();
  });

  it("affiche l'indicateur multi-jour quand multiDay = true", () => {
    const proposal = makeProposal({ multiDay: true });
    render(<TravelProposalCard proposal={proposal} index={0} onPress={onPress} />);
    expect(screen.getByText("+1j")).toBeTruthy();
  });

  it("n'affiche pas l'indicateur multi-jour quand multiDay = false", () => {
    render(<TravelProposalCard proposal={makeProposal()} index={0} onPress={onPress} />);
    expect(screen.queryByText("+1j")).toBeNull();
  });

  it("appelle onPress quand la carte est pressée", () => {
    render(<TravelProposalCard proposal={makeProposal()} index={0} onPress={onPress} />);
    fireEvent.press(screen.getByText("Départ"));
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it("utilise routeLongName quand routeShortName est vide", () => {
    const leg = makeLeg({ routeShortName: "", routeLongName: "Métro ligne 1" });
    const proposal = makeProposal({ legs: [leg] });
    render(<TravelProposalCard proposal={proposal} index={0} onPress={onPress} />);
    expect(screen.getByText("Métro ligne 1")).toBeTruthy();
  });
});
