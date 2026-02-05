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
      <View className="items-center mb-4 flex-row justify-center gap-2">
        <Navigation size={20} color="black" />
        <Text className="text-black text-center font-medium py-2 text-lg">
          {address.city}, {address.country}
        </Text>
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
      >
        <TouchableOpacity onPress={onShowPermissionModal} className="flex-row items-center gap-2 px-4 py-2 rounded-full">
          <X size={20} color="#dc2626" />
          <Text className="text-red-600 font-medium">Localisation refusée</Text>
        </TouchableOpacity>
      </Animated.View>
    );
  }

  return (
    <TouchableOpacity onPress={onRequestLocation} disabled={loadingLocation} className="flex-row items-center gap-2 px-4 py-2 bg-gray-100 rounded-full">
      <MapPin size={20} color={loadingLocation ? "#9ca3af" : "#000"} />
      <Text className={loadingLocation ? "text-gray-400" : "text-black"}>
        {loadingLocation ?
          <Spinner />
        : "Activer la localisation"}
      </Text>
    </TouchableOpacity>
  );
}
