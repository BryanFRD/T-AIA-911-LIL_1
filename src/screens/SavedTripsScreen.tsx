import React from "react";
import { View } from "react-native";
import { StatusBar } from "expo-status-bar";
import SavedTrips from "../components/travel/SavedTrips";
import Header from "@/components/voiceRecorder/Header";

export default function SavedTripsScreen() {
  return (
    <View className="flex-1 bg-[#F2F2F7]">
      <View className="pt-20 pb-4 px-6">
        <Header title="Travel Order Resolver" subtitle="Assistant IA vocal" />
      </View>
      <StatusBar style="dark" />
      <View className="flex-1 px-5">
        <SavedTrips />
      </View>
    </View>
  );
}
