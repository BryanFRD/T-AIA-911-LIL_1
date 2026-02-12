import React from "react";
import { View, Text, TouchableOpacity } from "react-native";
import { Navigation, ArrowRight, Repeat, ChevronRight, MoonStar } from "lucide-react-native";
import { Proposal } from "@/interfaces/solution.interface";

interface TravelProposalCardProps {
  proposal: Proposal;
  index: number;
  onPress: () => void;
}

const formatTime = (isoString: string): string => {
  const date = new Date(isoString);
  return date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
};

const formatDate = (isoString: string): string => {
  const date = new Date(isoString);
  return date.toLocaleDateString("fr-FR", { day: "numeric", month: "short" });
};

const formatTravelTime = (hours: number, minutes: number): string => {
  if (hours > 0 && minutes > 0) return `${hours}h${String(minutes).padStart(2, "0")}`;
  if (hours > 0) return `${hours}h`;
  return `${minutes} min`;
};

export default function TravelProposalCard({ proposal, index, onPress }: TravelProposalCardProps) {
  const firstLeg = proposal.legs[0];
  const lastLeg = proposal.legs[proposal.legs.length - 1];

  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.7}
      className="w-full bg-white rounded-2xl overflow-hidden mb-3"
      style={{
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.06,
        shadowRadius: 10,
        elevation: 2,
      }}
    >
      <View className="p-4">
        {/* Horaires départ → arrivée */}
        <View className="flex-row items-center justify-between mb-3">
          <View className="items-start">
            <Text className="text-[11px] text-gray-400 font-medium uppercase tracking-wider mb-0.5">Départ</Text>
            <Text className="text-[22px] font-bold text-black tracking-tight">{formatTime(proposal.departureTime)}</Text>
            <Text className="text-[11px] text-gray-400 mt-1.5">{formatDate(proposal.departureTime)}</Text>
          </View>

          {/* Ligne de trajet */}
          <View className="flex-1 mx-4 items-center">
            <View className="flex-row items-center w-full">
              <View className="w-2 h-2 rounded-full bg-[#007AFF]" />
              <View className="flex-1 h-[1.5px] bg-gray-200 mx-1 relative">
                {/* Points d'escale sur la ligne */}
                {proposal.legs.length > 1 &&
                  proposal.legs
                    .slice(1)
                    .map((_, i) => (
                      <View
                        key={i}
                        className="w-1.5 h-1.5 rounded-full bg-gray-300 absolute top-[-2px]"
                        style={{ left: `${((i + 1) / proposal.legs.length) * 100}%` }}
                      />
                    ))}
              </View>
              <View className="w-2 h-2 rounded-full bg-[#34C759]" />
            </View>
            <Text className="text-[11px] text-gray-400 mt-1.5">{formatTravelTime(proposal.travelTime.hours, proposal.travelTime.minutes)}</Text>
          </View>

          <View className="items-end">
            <Text className="text-[11px] text-gray-400 font-medium uppercase tracking-wider mb-0.5">Arrivée</Text>
            <Text className="text-[22px] font-bold text-black tracking-tight">{formatTime(proposal.arrivalTime)}</Text>
            <Text className="text-[11px] text-gray-400 mt-1.5">{formatDate(proposal.arrivalTime)}</Text>
          </View>
        </View>

        {/* Badges infos */}
        <View className="flex-row items-center justify-between">
          <View className="flex-row items-center gap-3">
            {/* Nombre de trajets */}
            <View className="flex-row items-center gap-1.5 bg-gray-200 rounded-full px-2 py-0.5">
              <Navigation size={14} color="#007AFF" />
              <Text> {proposal.legs.length} Trajets</Text>
            </View>

            <View className="flex-row items-center gap-3">
              {/* Noms des lignes */}
              <View className="flex-row items-center gap-1.5">
                {proposal.legs.map((leg, i) => (
                  <React.Fragment key={i}>
                    <View className="px-2 py-0.5 rounded-md" style={{ backgroundColor: leg.routeColor ? `#${leg.routeColor}` : "#007AFF" }}>
                      <Text className="text-white text-[11px] font-bold">{leg.routeShortName || leg.routeLongName}</Text>
                    </View>
                    {i < proposal.legs.length - 1 && <ArrowRight size={10} color="#C7C7CC" />}
                  </React.Fragment>
                ))}
              </View>
            </View>
          </View>

          <View className="flex-row items-center gap-3">
            {proposal.transferCount > 0 && (
              <View className="flex-row items-center gap-1">
                <Repeat size={12} color="#8E8E93" />
                <Text className="text-xs text-gray-400">{proposal.transferCount} corresp.</Text>
              </View>
            )}

            {proposal.multiDay && (
              <View className="flex-row items-center gap-1">
                <MoonStar size={12} color="#FF9500" />
                <Text className="text-xs text-[#FF9500]">+1j</Text>
              </View>
            )}

            <ChevronRight size={16} color="#C7C7CC" />
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );
}
