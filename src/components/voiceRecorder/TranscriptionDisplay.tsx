import React from "react";
import { View, Text, ScrollView, TouchableOpacity } from "react-native";
import { BotMessageSquare, Check, X } from "lucide-react-native";

interface TranscriptionDisplayProps {
  transcribedText: string;
}

export default function TranscriptionDisplay({ transcribedText }: TranscriptionDisplayProps) {
  return (
    <View className="mt-6 w-full border border-gray-300 rounded-3xl px-2">
      <View className="bg-white rounded-3xl overflow-hidden">
        <View className="flex-row items-center pt-5 pb-3">
          <View className="w-10 h-10 bg-blue-50 rounded-full items-center justify-center mr-3">
            <BotMessageSquare size={20} color="black" />
          </View>
          <Text className="text-lg font-semibold text-gray-900">Réponse de l'assistant</Text>
        </View>

        <View className="px-5 py-4 bg-gray-200 rounded-xl">
          {transcribedText ?
            <Text className="text-base leading-6 text-gray-700">{transcribedText}</Text>
          : <View className="items-center justify-center py-10">
              <View className="w-14 h-14 bg-gray-50 rounded-full items-center justify-center mb-3">
                <BotMessageSquare size={24} color="#9ca3af" />
              </View>
              <Text className="text-sm text-gray-400 text-center">En attente de transcription...</Text>
            </View>
          }
        </View>

        {/* Actions */}
        <View className=" py-4">
          <Text className="text-sm font-medium text-gray-500 mb-3">Confirmer votre demande</Text>
          <View className="flex-row gap-3">
            <TouchableOpacity className="flex-1 flex-row items-center justify-center py-3.5 bg-gray-500 rounded-2xl active:bg-gray-600" activeOpacity={0.8}>
              <Check size={18} color="white" />
              <Text className="text-white font-semibold ml-2">Confirmer</Text>
            </TouchableOpacity>

            <TouchableOpacity
              className="flex-1 flex-row items-center justify-center py-3.5 rounded-2xl border border-gray-300 active:bg-gray-200"
              activeOpacity={0.8}
            >
              <X size={18} color="#374151" />
              <Text className="text-gray-700 font-medium ml-2">Annuler</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </View>
  );
}
