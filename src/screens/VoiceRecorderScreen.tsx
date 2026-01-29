import React, { useState, useRef, useEffect } from "react";
import { View, Text, TouchableOpacity, Animated, Dimensions } from "react-native";
import { StatusBar } from "expo-status-bar";
import { Mic, ArrowUpFromDot } from "lucide-react-native";
import WaveBars from "../components/audio/AudioWave";

export default function VoiceRecorderScreen() {
  const [isRecording, setIsRecording] = useState(false);
  const modalAnim = useRef(new Animated.Value(0)).current;

  const screenWidth = Dimensions.get("window").width;
  const buttonSize = 56;
  const modalPadding = 16;

  const iconSize: number = 24;

  useEffect(() => {
    Animated.spring(modalAnim, {
      toValue: isRecording ? 1 : 0,
      useNativeDriver: false,
      tension: 65,
      friction: 10,
    }).start();
  }, [isRecording]);

  const handleRecordPress = () => {
    setIsRecording(!isRecording);
  };

  const modalWidth = modalAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [buttonSize + modalPadding * 2, screenWidth - 32],
  });

  return (
    <View className="flex-1">
      <StatusBar style="light" />

      <View className="flex-1 justify-center items-center px-4">
        <Text className="text-black font-bold text-2xl">Travel Order Resolver</Text>
        <Text className="text-black text-2xl mb-8">Voice Recorder</Text>
      </View>

      <View className="py-8 items-center">
        <Animated.View
          className={`py-2 rounded-full flex-row items-center justify-center overflow-hidden ${isRecording ? "bg-blue-500" : "border-2 border-black"}`}
          style={{ width: modalWidth }}
        >
          {isRecording && <WaveBars isRecording={isRecording} />}

          <TouchableOpacity onPress={handleRecordPress} activeOpacity={0.7} className={`p-4 rounded-full ${isRecording ? "bg-white/20 mx-2" : ""}`}>
            {isRecording ?
              <ArrowUpFromDot size={iconSize} color="white" />
            : <Mic size={iconSize} color="black" />}
          </TouchableOpacity>
        </Animated.View>
      </View>
    </View>
  );
}
