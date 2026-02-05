
import React from "react";
import { View, Text } from "react-native";
import Spinner from "../loading/LoadingSpinner";

interface LoadingViewProps {
  message?: string;
}

export default function LoadingView({ message = "Chargement en cours..." }: LoadingViewProps) {
  return (
    <View className="flex-1 justify-center items-center">
      <Spinner />
      <Text className="text-gray-600 text-base text-center mt-4">{message}</Text>
    </View>
  );
}
