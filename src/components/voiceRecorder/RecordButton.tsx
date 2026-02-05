import React, { useRef, useEffect } from "react";
import { View, TouchableOpacity, Animated, Dimensions, Easing } from "react-native";
import { Mic, ArrowUpFromDot, Ban } from "lucide-react-native";
import WaveBars from "../audio/AudioWave";

interface RecordButtonProps {
  isRecording: boolean;
  onSend: () => void;
  onCancel: () => void;
}

export default function RecordButton({ isRecording, onSend, onCancel }: RecordButtonProps) {
  const modalAnim = useRef(new Animated.Value(0)).current;

  const screenWidth = Dimensions.get("window").width;
  const buttonSize = 56;
  const modalPadding = 16;
  const iconSize = 24;

  useEffect(() => {
    if (isRecording) {
      Animated.sequence([
        Animated.timing(modalAnim, {
          toValue: 1.05,
          duration: 250,
          easing: Easing.bezier(0.25, 0.46, 0.45, 0.94),
          useNativeDriver: false,
        }),
        Animated.timing(modalAnim, {
          toValue: 1,
          duration: 150,
          easing: Easing.bezier(0.25, 0.46, 0.45, 0.94),
          useNativeDriver: false,
        }),
      ]).start();
    } else {
      Animated.timing(modalAnim, {
        toValue: 0,
        duration: 200,
        easing: Easing.bezier(0.25, 0.46, 0.45, 0.94),
        useNativeDriver: false,
      }).start();
    }
  }, [isRecording]);

  const modalWidth = modalAnim.interpolate({
    inputRange: [0, 1, 1.05],
    outputRange: [buttonSize + modalPadding * 2, screenWidth - 32, (screenWidth - 32) * 1.05],
  });

  return (
    <View className="pb-8 flex items-center">
      <Animated.View
        className={`rounded-full flex-row items-center justify-center overflow-hidden ${isRecording ? "bg-gray-200" : "border-2 border-black"}`}
        style={{ width: modalWidth }}
      >
        {isRecording && <WaveBars isRecording={isRecording} />}

        <TouchableOpacity onPress={onSend} activeOpacity={0.7} className={`p-4 rounded-full ${isRecording ? "bg-gray-300 mx-2" : ""}`}>
          {isRecording ?
            <ArrowUpFromDot size={iconSize} color="white" />
          : <Mic size={iconSize} color="black" />}
        </TouchableOpacity>

        {isRecording && (
          <TouchableOpacity onPress={onCancel} className="flex-row items-center p-4 my-2 mr-2 rounded-full bg-red-400">
            <Ban size={iconSize} color="white" />
          </TouchableOpacity>
        )}
      </Animated.View>
    </View>
  );
}
