import React from "react";
import { View, Text, TouchableOpacity, Animated } from "react-native";
import { Navigation, MapPin, X } from "lucide-react-native";
import Spinner from "../loading/LoadingSpinner";

interface LocationDisplayProps {
  address: { city: string; country: string } | null;
  permissionDenied: boolean;
  loadingLocation: boolean;
  fadeAnim: Animated.Value;
  slideAnim: Animated.Value;
  onRequestLocation: () => void;
  onShowPermissionModal: () => void;
}

export default function LocationDisplay({
  address,
  permissionDenied,
  loadingLocation,
  fadeAnim,
  slideAnim,
  onRequestLocation,
  onShowPermissionModal,
}: LocationDisplayProps) {
  if (address) {
    return (
      <View
        className="w-full bg-white rounded-2xl p-4 flex-row items-center gap-3"
        style={{ shadowColor: "#000", shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 10, elevation: 2 }}
      >
        <View className="w-10 h-10 bg-[#007AFF]/10 rounded-xl items-center justify-center">
          <Navigation size={18} color="#007AFF" />
        </View>
        <View className="flex-1">
          <Text className="text-gray-400 text-xs font-medium uppercase tracking-wider mb-0.5">Position actuelle</Text>
          <Text className="text-black font-semibold text-base">
            {address.city}, {address.country}
          </Text>
        </View>
      </View>
    );
  }

  if (permissionDenied) {
    return (
      <Animated.View
        style={{
          opacity: fadeAnim,
          transform: [{ translateY: slideAnim }],
        }}
        className="w-full"
      >
        <TouchableOpacity
          onPress={onShowPermissionModal}
          className="w-full bg-white rounded-2xl p-4 flex-row items-center gap-3"
          style={{ shadowColor: "#000", shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 10, elevation: 2 }}
        >
          <View className="w-10 h-10 bg-red-50 rounded-xl items-center justify-center">
            <X size={18} color="#FF3B30" />
          </View>
          <View className="flex-1">
            <Text className="text-red-500 font-medium text-base">Localisation refusée</Text>
            <Text className="text-gray-400 text-xs mt-0.5">Appuyez pour réactiver</Text>
          </View>
        </TouchableOpacity>
      </Animated.View>
    );
  }

  return (
    <TouchableOpacity
      onPress={onRequestLocation}
      disabled={loadingLocation}
      className="w-full bg-white rounded-2xl p-4 flex-row items-center gap-3"
      style={{ shadowColor: "#000", shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 10, elevation: 2 }}
    >
      {loadingLocation ?
        <Spinner />
      : <>
          <View className="w-10 h-10 bg-[#007AFF]/10 rounded-xl items-center justify-center">
            <MapPin size={18} color="#007AFF" />
          </View>
          <View className="flex-1">
            <Text className="text-black font-medium text-base">Activer la localisation</Text>
            <Text className="text-gray-400 text-xs mt-0.5">Pour améliorer la précision</Text>
          </View>
        </>
      }
    </TouchableOpacity>
  );
}
