import React from "react";
import { render, screen } from "@testing-library/react-native";
import RecordingTimer from "./RecordingTimer";

describe("RecordingTimer", () => {
  it("affiche 0:00:00 pour timer = 0", () => {
    render(<RecordingTimer timer={0} />);
    expect(screen.getByText("0:00:00")).toBeTruthy();
  });

  it("affiche le temps formaté pour 65 secondes", () => {
    render(<RecordingTimer timer={65} />);
    expect(screen.getByText("0:01:05")).toBeTruthy();
  });

  it("affiche le temps formaté avec des heures", () => {
    render(<RecordingTimer timer={3725} />);
    expect(screen.getByText("1:02:05")).toBeTruthy();
  });

  it("affiche 0:00:59 pour 59 secondes", () => {
    render(<RecordingTimer timer={59} />);
    expect(screen.getByText("0:00:59")).toBeTruthy();
  });
});
