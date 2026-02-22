import React from "react";
import { View, Text, TouchableOpacity, Modal, Linking, Platform } from "react-native";
import { X, Settings } from "lucide-react-native";

interface LocationPermissionModalProps {
  visible: boolean;
  onClose: () => void;
}

export default function LocationPermissionModal({ visible, onClose }: LocationPermissionModalProps) {
  const openSettings = () => {
    if (Platform.OS === "ios") {
      Linking.openURL("app-settings:");
    } else {
      Linking.openSettings();
    }
    onClose();
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View className="flex-1 justify-center items-center bg-black/50 px-6">
        <View className="bg-white rounded-2xl p-6 w-full max-w-sm">
          <View className="flex-row justify-between items-center mb-4">
            <Text className="text-xl font-bold text-black">Localisation désactivée</Text>
            <TouchableOpacity onPress={onClose}>
              <X size={24} color="#6b7280" />
            </TouchableOpacity>
          </View>

          <Text className="text-gray-600 mb-4 leading-6">
            Pour utiliser la localisation, vous devez autoriser l'accès dans les paramètres de votre appareil.
          </Text>

          <Text className="text-gray-600 mb-6 leading-6">
            Allez dans <Text className="font-semibold">Paramètres → Applications → Travel Order Resolver → Autorisations → Localisation</Text> et activez
            l'accès.
          </Text>

          <TouchableOpacity onPress={openSettings} className="bg-blue-500 rounded-xl py-3 flex-row items-center justify-center gap-2">
            <Settings size={20} color="white" />
            <Text className="text-white font-semibold text-base">Ouvrir les paramètres</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}
