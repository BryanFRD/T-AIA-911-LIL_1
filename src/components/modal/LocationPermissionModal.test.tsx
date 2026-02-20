import React from "react";
import { Linking } from "react-native";
import { render, screen, fireEvent } from "@testing-library/react-native";
import LocationPermissionModal from "./LocationPermissionModal";

jest.mock("lucide-react-native", () =>
  new Proxy({}, { get: () => () => null })
);

jest.mock("react-native", () => {
  const rn = jest.requireActual("react-native");
  rn.Linking.openURL = jest.fn().mockResolvedValue(undefined);
  rn.Linking.openSettings = jest.fn().mockResolvedValue(undefined);
  return rn;
});

describe("LocationPermissionModal", () => {
  const onClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("affiche le contenu quand visible = true", () => {
    render(<LocationPermissionModal visible={true} onClose={onClose} />);
    expect(screen.getByText("Localisation désactivée")).toBeTruthy();
  });

  it("n'affiche pas le contenu quand visible = false", () => {
    render(<LocationPermissionModal visible={false} onClose={onClose} />);
    expect(screen.queryByText("Localisation désactivée")).toBeNull();
  });

  it("affiche le message d'explication", () => {
    render(<LocationPermissionModal visible={true} onClose={onClose} />);
    expect(
      screen.getByText(
        "Pour utiliser la localisation, vous devez autoriser l'accès dans les paramètres de votre appareil."
      )
    ).toBeTruthy();
  });

  it("affiche le bouton Ouvrir les paramètres", () => {
    render(<LocationPermissionModal visible={true} onClose={onClose} />);
    expect(screen.getByText("Ouvrir les paramètres")).toBeTruthy();
  });

  it("appelle onClose quand le bouton X est pressé", () => {
    render(<LocationPermissionModal visible={true} onClose={onClose} />);
    const buttons = screen.UNSAFE_getAllByType(
      require("react-native").TouchableOpacity
    );
    // Premier bouton = X (fermer)
    fireEvent.press(buttons[0]);
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("appelle onClose après avoir ouvert les paramètres", () => {
    render(<LocationPermissionModal visible={true} onClose={onClose} />);
    fireEvent.press(screen.getByText("Ouvrir les paramètres"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
