import React from "react";
import { Animated } from "react-native";
import { render, screen, fireEvent } from "@testing-library/react-native";
import LocationDisplay from "./LocationDisplay";

jest.mock("lucide-react-native", () =>
  new Proxy({}, { get: () => () => null })
);
jest.mock("../loading/LoadingSpinner", () => () => null);

const makeProps = (overrides = {}) => ({
  address: null,
  permissionDenied: false,
  loadingLocation: false,
  fadeAnim: new Animated.Value(1),
  slideAnim: new Animated.Value(0),
  onRequestLocation: jest.fn(),
  onShowPermissionModal: jest.fn(),
  ...overrides,
});

describe("LocationDisplay", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("quand l'adresse est disponible", () => {
    it("affiche la ville et le pays", () => {
      render(
        <LocationDisplay
          {...makeProps({ address: { city: "Paris", country: "France" } })}
        />
      );
      expect(screen.getByText("Paris, France")).toBeTruthy();
    });

    it("affiche le label 'Position actuelle'", () => {
      render(
        <LocationDisplay
          {...makeProps({ address: { city: "Lyon", country: "France" } })}
        />
      );
      expect(screen.getByText("Position actuelle")).toBeTruthy();
    });
  });

  describe("quand la permission est refusée", () => {
    it("affiche 'Localisation refusée'", () => {
      render(<LocationDisplay {...makeProps({ permissionDenied: true })} />);
      expect(screen.getByText("Localisation refusée")).toBeTruthy();
    });

    it("affiche l'invite à réactiver", () => {
      render(<LocationDisplay {...makeProps({ permissionDenied: true })} />);
      expect(screen.getByText("Appuyez pour réactiver")).toBeTruthy();
    });

    it("appelle onShowPermissionModal quand pressé", () => {
      const onShowPermissionModal = jest.fn();
      render(
        <LocationDisplay
          {...makeProps({ permissionDenied: true, onShowPermissionModal })}
        />
      );
      fireEvent.press(screen.getByText("Localisation refusée"));
      expect(onShowPermissionModal).toHaveBeenCalledTimes(1);
    });
  });

  describe("état par défaut (pas de localisation)", () => {
    it("affiche 'Activer la localisation'", () => {
      render(<LocationDisplay {...makeProps()} />);
      expect(screen.getByText("Activer la localisation")).toBeTruthy();
    });

    it("affiche le texte descriptif", () => {
      render(<LocationDisplay {...makeProps()} />);
      expect(screen.getByText("Pour améliorer la précision")).toBeTruthy();
    });

    it("appelle onRequestLocation quand pressé", () => {
      const onRequestLocation = jest.fn();
      render(<LocationDisplay {...makeProps({ onRequestLocation })} />);
      fireEvent.press(screen.getByText("Activer la localisation"));
      expect(onRequestLocation).toHaveBeenCalledTimes(1);
    });

    it("désactive le bouton quand loadingLocation = true", () => {
      render(<LocationDisplay {...makeProps({ loadingLocation: true })} />);
      expect(screen.queryByText("Activer la localisation")).toBeNull();
    });
  });
});
