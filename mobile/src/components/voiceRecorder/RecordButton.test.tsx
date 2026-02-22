import React from "react";
import { render, screen, fireEvent } from "@testing-library/react-native";
import RecordButton from "./RecordButton";

jest.mock("lucide-react-native", () =>
  new Proxy({}, { get: () => () => null })
);
jest.mock("../audio/AudioWave", () => () => null);

const defaultProps = {
  isRecording: false,
  onSend: jest.fn(),
  onCancel: jest.fn(),
};

describe("RecordButton", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("état non-enregistrement", () => {
    it("affiche un seul bouton (send) quand pas en enregistrement", () => {
      const onSend = jest.fn();
      render(<RecordButton {...defaultProps} onSend={onSend} />);
      // Un seul TouchableOpacity visible (le bouton principal)
      const buttons = screen.UNSAFE_getAllByType(
        require("react-native").TouchableOpacity
      );
      expect(buttons).toHaveLength(1);
    });

    it("n'affiche pas le bouton annuler", () => {
      render(<RecordButton {...defaultProps} isRecording={false} />);
      const buttons = screen.UNSAFE_getAllByType(
        require("react-native").TouchableOpacity
      );
      expect(buttons).toHaveLength(1);
    });
  });

  describe("état enregistrement", () => {
    it("affiche deux boutons (send + cancel) pendant l'enregistrement", () => {
      render(<RecordButton {...defaultProps} isRecording={true} />);
      const buttons = screen.UNSAFE_getAllByType(
        require("react-native").TouchableOpacity
      );
      expect(buttons).toHaveLength(2);
    });
  });

  describe("interactions", () => {
    it("appelle onSend quand le bouton principal est pressé (pas en enregistrement)", () => {
      const onSend = jest.fn();
      render(<RecordButton {...defaultProps} onSend={onSend} />);
      const [sendButton] = screen.UNSAFE_getAllByType(
        require("react-native").TouchableOpacity
      );
      fireEvent.press(sendButton);
      expect(onSend).toHaveBeenCalledTimes(1);
    });

    it("appelle onSend quand le bouton send est pressé (en enregistrement)", () => {
      const onSend = jest.fn();
      render(<RecordButton {...defaultProps} isRecording={true} onSend={onSend} />);
      const [sendButton] = screen.UNSAFE_getAllByType(
        require("react-native").TouchableOpacity
      );
      fireEvent.press(sendButton);
      expect(onSend).toHaveBeenCalledTimes(1);
    });

    it("appelle onCancel quand le bouton annuler est pressé", () => {
      const onCancel = jest.fn();
      render(<RecordButton {...defaultProps} isRecording={true} onCancel={onCancel} />);
      const buttons = screen.UNSAFE_getAllByType(
        require("react-native").TouchableOpacity
      );
      fireEvent.press(buttons[1]);
      expect(onCancel).toHaveBeenCalledTimes(1);
    });
  });
});
