import React from "react";
import { View, Text, TouchableOpacity } from "react-native";
import { Play, Pause } from "lucide-react-native";

interface PlaybackControlsProps {
  isPlaying: boolean;
  onTogglePlayback: () => void;
}

export default function PlaybackControls({ isPlaying, onTogglePlayback }: PlaybackControlsProps) {
  return (
    <View className="items-center mb-4">
      <TouchableOpacity onPress={onTogglePlayback} className="flex-row items-center gap-3 px-6 py-3 bg-[#007AFF] rounded-full">
        {isPlaying ?
          <Pause size={24} color="white" fill="white" />
        : <Play size={24} color="white" fill="white" />}
        <Text className="text-white font-semibold text-lg">{isPlaying ? "Pause" : "Écouter l'enregistrement"}</Text>
      </TouchableOpacity>
    </View>
  );
}
