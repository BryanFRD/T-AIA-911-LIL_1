import React, { useState, useRef, useEffect } from "react";
import { View, Text, TouchableOpacity, Animated, Dimensions } from "react-native";
import { StatusBar } from "expo-status-bar";
import { Mic, ArrowUpFromDot, Navigation } from "lucide-react-native";
import WaveBars from "../components/audio/AudioWave";

export default function VoiceRecorderScreen() {
  const [isRecording, setIsRecording] = useState(false);
  const [timer, setTimer] = useState(0);
  const [localisation, setLocalisation] = useState<string | null>("Paris, France");
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const modalAnim = useRef(new Animated.Value(0)).current;

  const screenWidth = Dimensions.get("window").width;
  const buttonSize = 56;
  const modalPadding = 16;

  const iconSize: number = 24;

  useEffect(() => {
    if (isRecording) {
      intervalRef.current = setInterval(() => {
        setTimer((s) => s + 1);
      }, 1000);
    }

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isRecording]);

  useEffect(() => {
    if (!isRecording) {
      setTimer(0);
    }
  });

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

  const formatTime = (totalSeconds: number) => {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;

    return `${hours}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
  };

  const modalWidth = modalAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [buttonSize + modalPadding * 2, screenWidth - 32],
  });

  return (
    <View className="flex-1">
      <StatusBar style="light" />
      <View className="flex-1 justify-center items-center px-4 gap-2">
        <Text className="text-black font-bold text-2xl">Travel Order Resolver</Text>
        <Text className="text-black text-2xl ">Voice Recorder</Text>

        {localisation && (
          <View className=" text-center items-center mb-4 flex-row justify-center gap-2">
            <Navigation size={20} color="black" className="self-center mb-1" />
            <Text className="text-black text-center font-medium py-2 text-lg">{localisation}</Text>
          </View>
        )}
      </View>

      {isRecording && (
        <View>
          <Text className="text-black text-center font-bold py-2 text-2xl">{formatTime(timer)}</Text>
        </View>
      )}
      <View className="pb-8 items-center">
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
