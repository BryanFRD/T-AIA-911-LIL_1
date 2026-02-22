import React from "react";
import { View, Text, TouchableOpacity, ActivityIndicator } from "react-native";
import { BotMessageSquare, Check, X } from "lucide-react-native";

interface TranscriptionDisplayProps {
  transcribedText: string;
  onConfirm: () => void;
  onCancel: () => void;
  isLoadingSolution: boolean;
}

const formatTranscribedText = (text: string): string => {
  return text.charAt(0).toUpperCase() + text.slice(1);
};

export default function TranscriptionDisplay({ transcribedText, onConfirm, onCancel, isLoadingSolution }: TranscriptionDisplayProps) {
  return (
    <View
      className="w-full bg-white rounded-2xl overflow-hidden"
      style={{ shadowColor: "#000", shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 10, elevation: 2 }}
    >
      <View className="flex-row items-center p-4 pb-3">
        <View className="w-9 h-9 bg-[#007AFF]/10 rounded-xl items-center justify-center mr-3">
          <BotMessageSquare size={18} color="#007AFF" />
        </View>
        <Text className="text-base font-semibold text-black">Réponse de l'assistant</Text>
      </View>

      <View className="mx-4 mb-4 px-4 py-3.5 bg-[#F2F2F7] rounded-xl">
        {transcribedText ?
          <Text className="text-[15px] leading-6 text-gray-700">{formatTranscribedText(transcribedText)}</Text>
        : <View className="items-center justify-center py-8">
            <View className="w-12 h-12 bg-gray-200 rounded-full items-center justify-center mb-3">
              <BotMessageSquare size={22} color="#9ca3af" />
            </View>
            <Text className="text-sm text-gray-400 text-center">En attente de transcription...</Text>
          </View>
        }
      </View>

      {/* Actions */}
      <View className="px-4 pb-4">
        {isLoadingSolution ?
          <View className="flex-row items-center justify-center py-3 gap-2">
            <ActivityIndicator size="small" color="#007AFF" />
            <Text className="text-[15px] text-gray-400 font-medium">Recherche en cours...</Text>
          </View>
        : <View className="flex-row gap-3">
            <TouchableOpacity onPress={onCancel} className="flex-1 flex-row items-center justify-center py-3 rounded-xl bg-[#F2F2F7]" activeOpacity={0.7}>
              <X size={16} color="#8E8E93" />
              <Text className="text-gray-500 font-medium ml-1.5 text-[15px]">Annuler</Text>
            </TouchableOpacity>

            <TouchableOpacity onPress={onConfirm} className="flex-1 flex-row items-center justify-center py-3 bg-[#007AFF] rounded-xl" activeOpacity={0.7}>
              <Check size={16} color="white" />
              <Text className="text-white font-semibold ml-1.5 text-[15px]">Confirmer</Text>
            </TouchableOpacity>
          </View>
        }
      </View>
    </View>
  );
}
