import React, { useState, useCallback } from "react";
import { View, Text, ScrollView, TouchableOpacity, Alert, RefreshControl } from "react-native";
import { Bookmark, Trash2, Clock, Train, Repeat, ChevronRight } from "lucide-react-native";
import { useSavedProposals, SavedProposal } from "@/hooks/useSavedProposals";
import ProposalDetail from "./ProposalDetail";
import { Proposal } from "@/interfaces/solution.interface";

const formatTime = (isoOrString: string): string => {
  if (isoOrString.includes("T")) {
    const date = new Date(isoOrString);
    return date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
  }
  return isoOrString;
};

const formatDate = (isoString: string): string => {
  if (isoString.includes("T")) {
    const date = new Date(isoString);
    return date.toLocaleDateString("fr-FR", { day: "numeric", month: "short" });
  }
  return isoString;
};

const formatTravelTime = (hours: number, minutes: number): string => {
  if (hours > 0 && minutes > 0) return `${hours}h${String(minutes).padStart(2, "0")}`;
  if (hours > 0) return `${hours}h`;
  return `${minutes} min`;
};

export default function SavedTrips() {
  const { savedProposals, loading, removeSaved, isSaved, toggleSave, refresh } = useSavedProposals();
  const [selectedProposal, setSelectedProposal] = useState<Proposal | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await refresh();
    setRefreshing(false);
  }, [refresh]);

  const handleDelete = (item: SavedProposal) => {
    Alert.alert("Supprimer", "Voulez-vous supprimer ce trajet sauvegardé ?", [
      { text: "Annuler", style: "cancel" },
      { text: "Supprimer", style: "destructive", onPress: () => removeSaved(item.id) },
    ]);
  };

  if (selectedProposal) {
    return (
      <ProposalDetail
        proposal={selectedProposal}
        onBack={() => setSelectedProposal(null)}
        isSaved={isSaved(selectedProposal)}
        onToggleSave={() => toggleSave(selectedProposal)}
      />
    );
  }

  return (
    <View className="flex-1">
      {/* Header */}
      <View className="flex-row items-center mb-4">
        <Bookmark size={20} color="#007AFF" />
        <Text className="text-lg font-bold text-black ml-2">Trajets sauvegardés</Text>
      </View>

      {loading ?
        <View className="flex-1 items-center justify-center">
          <Text className="text-gray-400 text-[15px]">Chargement…</Text>
        </View>
      : savedProposals.length === 0 ?
        <View className="flex-1 items-center justify-center px-8">
          <Bookmark size={48} color="#D1D5DB" />
          <Text className="text-gray-400 text-[17px] font-semibold mt-4 text-center">Aucun trajet sauvegardé</Text>
          <Text className="text-gray-300 text-[14px] mt-2 text-center">Sauvegardez des propositions de trajet pour les retrouver ici.</Text>
        </View>
      : <ScrollView
          className="flex-1"
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#007AFF" />}
        >
          {savedProposals.map((item) => {
            const { proposal } = item;
            const firstLeg = proposal.legs[0];
            const lastLeg = proposal.legs[proposal.legs.length - 1];

            return (
              <TouchableOpacity
                key={item.id}
                onPress={() => setSelectedProposal(proposal)}
                activeOpacity={0.7}
                className="bg-white rounded-2xl mb-3 overflow-hidden"
                style={{
                  shadowColor: "#000",
                  shadowOffset: { width: 0, height: 1 },
                  shadowOpacity: 0.06,
                  shadowRadius: 10,
                  elevation: 2,
                }}
              >
                <View className="p-4">
                  {/* Origin → Destination */}
                  <View className="flex-row items-center mb-2">
                    <View className="flex-1">
                      <View className="flex-row items-center gap-2">
                        <View className="w-2 h-2 rounded-full bg-[#007AFF]" />
                        <Text className="text-[14px] font-semibold text-black" numberOfLines={1}>
                          {firstLeg?.originStopName}
                        </Text>
                      </View>
                      <View className="flex-row items-center gap-2 mt-1">
                        <View className="w-2 h-2 rounded-full bg-[#34C759]" />
                        <Text className="text-[14px] font-semibold text-black" numberOfLines={1}>
                          {lastLeg?.destinationStopName}
                        </Text>
                      </View>
                    </View>
                    <ChevronRight size={18} color="#C7C7CC" />
                  </View>

                  {/* Time info */}
                  <View className="flex-row items-center justify-between mt-2">
                    <View className="flex-row items-center gap-3">
                      <Text className="text-[18px] font-bold text-black">{formatTime(proposal.departureTime)}</Text>
                      <Text className="text-gray-300">→</Text>
                      <Text className="text-[18px] font-bold text-black">{formatTime(proposal.arrivalTime)}</Text>
                    </View>
                    <View className="flex-row items-center gap-1.5">
                      <Clock size={12} color="#8E8E93" />
                      <Text className="text-[12px] text-gray-400">{formatTravelTime(proposal.travelTime.hours, proposal.travelTime.minutes)}</Text>
                    </View>
                  </View>

                  {/* Route badges + delete */}
                  <View className="flex-row items-center justify-between mt-3">
                    <View className="flex-row items-center gap-2 flex-1">
                      {proposal.legs.map((leg, i) => (
                        <View
                          key={i}
                          className="flex-row items-center px-2 py-0.5 rounded-md gap-1"
                          style={{ backgroundColor: leg.routeColor ? `#${leg.routeColor}` : "#007AFF" }}
                        >
                          <Train size={10} color="white" />
                          <Text className="text-white text-[11px] font-bold">{leg.routeShortName || "—"}</Text>
                        </View>
                      ))}
                      {proposal.transferCount > 0 && (
                        <View className="flex-row items-center gap-1">
                          <Repeat size={11} color="#8E8E93" />
                          <Text className="text-[11px] text-gray-400">{proposal.transferCount}</Text>
                        </View>
                      )}
                    </View>
                    <TouchableOpacity onPress={() => handleDelete(item)} hitSlop={8} className="ml-2 p-1.5">
                      <Trash2 size={16} color="#FF3B30" />
                    </TouchableOpacity>
                  </View>

                  {/* Saved date */}
                  <Text className="text-[11px] text-gray-300 mt-2">Sauvegardé le {formatDate(item.savedAt)}</Text>
                </View>
              </TouchableOpacity>
            );
          })}
          <View className="h-4" />
        </ScrollView>
      }
    </View>
  );
}
