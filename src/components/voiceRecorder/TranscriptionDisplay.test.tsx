import React from "react";
import { render, screen, fireEvent } from "@testing-library/react-native";
import TranscriptionDisplay from "./TranscriptionDisplay";

jest.mock("lucide-react-native", () =>
  new Proxy({}, { get: () => () => null })
);

const defaultProps = {
  transcribedText: "aller à la gare de lyon",
  onConfirm: jest.fn(),
  onCancel: jest.fn(),
  isLoadingSolution: false,
};

describe("TranscriptionDisplay", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("affiche le texte transcrit avec la 1ère lettre en majuscule", () => {
    render(<TranscriptionDisplay {...defaultProps} />);
    expect(screen.getByText("Aller à la gare de lyon")).toBeTruthy();
  });

  it("affiche le message d'attente quand le texte est vide", () => {
    render(<TranscriptionDisplay {...defaultProps} transcribedText="" />);
    expect(screen.getByText("En attente de transcription...")).toBeTruthy();
  });

  it("affiche les boutons Confirmer et Annuler quand pas en chargement", () => {
    render(<TranscriptionDisplay {...defaultProps} />);
    expect(screen.getByText("Confirmer")).toBeTruthy();
    expect(screen.getByText("Annuler")).toBeTruthy();
  });

  it("n'affiche pas les boutons quand isLoadingSolution = true", () => {
    render(<TranscriptionDisplay {...defaultProps} isLoadingSolution={true} />);
    expect(screen.queryByText("Confirmer")).toBeNull();
    expect(screen.queryByText("Annuler")).toBeNull();
  });

  it("affiche l'indicateur de chargement quand isLoadingSolution = true", () => {
    render(<TranscriptionDisplay {...defaultProps} isLoadingSolution={true} />);
    expect(screen.getByText("Recherche en cours...")).toBeTruthy();
  });

  it("appelle onConfirm quand le bouton Confirmer est pressé", () => {
    const onConfirm = jest.fn();
    render(<TranscriptionDisplay {...defaultProps} onConfirm={onConfirm} />);
    fireEvent.press(screen.getByText("Confirmer"));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("appelle onCancel quand le bouton Annuler est pressé", () => {
    const onCancel = jest.fn();
    render(<TranscriptionDisplay {...defaultProps} onCancel={onCancel} />);
    fireEvent.press(screen.getByText("Annuler"));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("affiche le titre de l'assistant", () => {
    render(<TranscriptionDisplay {...defaultProps} />);
    expect(screen.getByText("Réponse de l'assistant")).toBeTruthy();
  });
});
