import React, { useState, useRef, useEffect } from "react";
import { View, Text, TouchableOpacity, Animated, Dimensions } from "react-native";
import { StatusBar } from "expo-status-bar";
import { Mic, ArrowUpFromDot, Navigation, MapPin, X, Play, Pause } from "lucide-react-native";
import WaveBars from "../components/audio/AudioWave";
import Spinner from "../components/loading/LoadingSpinner";
import LocationPermissionModal from "../components/modal/LocationPermissionModal";
import { useVoiceRecorder } from "../hooks/useVoiceRecorder";
import { useLocation } from "../hooks/useLocation";
import { formatTime } from "../utils/timeHelper";

export default function VoiceRecorderScreen() {
  const [showPermissionModal, setShowPermissionModal] = useState(false);

  const { isRecording, recordingUri, isPlaying, timer, toggleRecording, togglePlayback } = useVoiceRecorder();

  const { address, loadingLocation, permissionDenied, fadeAnim, slideAnim, requestLocation } = useLocation();

  const modalAnim = useRef(new Animated.Value(0)).current;

  const screenWidth = Dimensions.get("window").width;
  const buttonSize = 56;
  const modalPadding = 16;
  const iconSize = 24;

  useEffect(() => {
    Animated.spring(modalAnim, {
      toValue: isRecording ? 1 : 0,
      useNativeDriver: false,
      tension: 65,
      friction: 10,
    }).start();
  }, [isRecording]);

  const modalWidth = modalAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [buttonSize + modalPadding * 2, screenWidth - 32],
  });

  return (
    <View className="flex-1 pt-24 bg-white">
      <StatusBar style="light" />

      <LocationPermissionModal visible={showPermissionModal} onClose={() => setShowPermissionModal(false)} />

      <View className="flex-1 justify-center items-center px-4 gap-2">
        <Text className="text-black font-bold text-2xl">Travel Order Resolver</Text>
        <Text className="text-black text-2xl">Voice Recorder</Text>

        {address ?
          <View className="items-center mb-4 flex-row justify-center gap-2">
            <Navigation size={20} color="black" />
            <Text className="text-black text-center font-medium py-2 text-lg">
              {address.city}, {address.country}
            </Text>
          </View>
        : permissionDenied ?
          <Animated.View
            style={{
              opacity: fadeAnim,
              transform: [{ translateY: slideAnim }],
            }}
          >
            <TouchableOpacity onPress={() => setShowPermissionModal(true)} className="flex-row items-center gap-2 px-4 py-2 rounded-full">
              <X size={20} color="#dc2626" />
              <Text className="text-red-600 font-medium">Localisation refusée</Text>
            </TouchableOpacity>
          </Animated.View>
        : <TouchableOpacity onPress={requestLocation} disabled={loadingLocation} className="flex-row items-center gap-2 px-4 py-2 bg-gray-100 rounded-full">
            <MapPin size={20} color={loadingLocation ? "#9ca3af" : "#000"} />
            <Text className={loadingLocation ? "text-gray-400" : "text-black"}>
              {loadingLocation ?
                <Spinner />
              : "Activer la localisation"}
            </Text>
          </TouchableOpacity>
        }
      </View>

      {isRecording && (
        <View>
          <Text className="text-black text-center font-bold py-2 text-2xl">{formatTime(timer)}</Text>
        </View>
      )}

      {!isRecording && recordingUri && (
        <View className="items-center mb-4">
          <TouchableOpacity onPress={togglePlayback} className="flex-row items-center gap-3 px-6 py-3 bg-blue-500 rounded-full">
            {isPlaying ?
              <Pause size={24} color="white" fill="white" />
            : <Play size={24} color="white" fill="white" />}
            <Text className="text-white font-semibold text-lg">{isPlaying ? "Pause" : "Écouter l'enregistrement"}</Text>
          </TouchableOpacity>
        </View>
      )}

      <View className="pb-8 items-center">
        <Animated.View
          className={`py-2 rounded-full flex-row items-center justify-center overflow-hidden ${isRecording ? "bg-blue-500" : "border-2 border-black"}`}
          style={{ width: modalWidth }}
        >
          {isRecording && <WaveBars isRecording={isRecording} />}

          <TouchableOpacity onPress={toggleRecording} activeOpacity={0.7} className={`p-4 rounded-full ${isRecording ? "bg-white/20 mx-2" : ""}`}>
            {isRecording ?
              <ArrowUpFromDot size={iconSize} color="white" />
            : <Mic size={iconSize} color="black" />}
          </TouchableOpacity>
        </Animated.View>
      </View>
    </View>
  );
}
