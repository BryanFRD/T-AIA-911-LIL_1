import React from "react";
import { render, screen } from "@testing-library/react-native";
import Header from "./Header";

describe("Header", () => {
  it("affiche le titre", () => {
    render(<Header title="Mon Titre" subtitle="Mon sous-titre" />);
    expect(screen.getByText("Mon Titre")).toBeTruthy();
  });

  it("affiche le sous-titre", () => {
    render(<Header title="Mon Titre" subtitle="Mon sous-titre" />);
    expect(screen.getByText("Mon sous-titre")).toBeTruthy();
  });

  it("affiche les deux textes simultanément", () => {
    render(<Header title="Travel" subtitle="Recherchez un itinéraire" />);
    expect(screen.getByText("Travel")).toBeTruthy();
    expect(screen.getByText("Recherchez un itinéraire")).toBeTruthy();
  });
});
