import React from "react";
import { View, Text, ScrollView, TouchableOpacity } from "react-native";
import { MapPin, Clock, ArrowLeft, Repeat, MoonStar, Train, ChevronDown, Bookmark, BookmarkCheck } from "lucide-react-native";
import { Proposal } from "@/interfaces/solution.interface";

interface ProposalDetailProps {
  proposal: Proposal;
  onBack: () => void;
  isSaved: boolean;
  onToggleSave: () => void;
}

const formatTime = (isoOrString: string): string => {
  // Handle both ISO date strings and plain time strings
  if (isoOrString.includes("T")) {
    const date = new Date(isoOrString);
    return date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
  }
  return isoOrString;
};

const formatTravelTime = (hours: number, minutes: number): string => {
  if (hours > 0 && minutes > 0) return `${hours}h${String(minutes).padStart(2, "0")}`;
  if (hours > 0) return `${hours}h`;
  return `${minutes} min`;
};

export default function ProposalDetail({ proposal, onBack, isSaved, onToggleSave }: ProposalDetailProps) {
  return (
    <View className="flex-1">
      {/* Header */}
      <View className="flex-row items-center mb-4">
        <TouchableOpacity onPress={onBack} className="mr-3 p-1">
          <ArrowLeft size={22} color="#007AFF" />
        </TouchableOpacity>
        <Text className="text-lg font-bold text-black">Détail du trajet</Text>
      </View>

      {/* Summary card */}
      <View
        className="bg-white rounded-2xl p-4 mb-4"
        style={{
          shadowColor: "#000",
          shadowOffset: { width: 0, height: 1 },
          shadowOpacity: 0.06,
          shadowRadius: 10,
          elevation: 2,
        }}
      >
        <View className="flex-row items-center justify-between mb-2">
          <View className="flex-row items-center gap-2">
            <Clock size={14} color="#8E8E93" />
            <Text className="text-[15px] font-semibold text-black">{formatTravelTime(proposal.travelTime.hours, proposal.travelTime.minutes)}</Text>
          </View>
          <View className="flex-row items-center gap-3">
            {proposal.transferCount > 0 && (
              <View className="flex-row items-center gap-1">
                <Repeat size={13} color="#8E8E93" />
                <Text className="text-[13px] text-gray-400">
                  {proposal.transferCount} {proposal.transferCount > 1 ? "correspondances" : "correspondance"}
                </Text>
              </View>
            )}
            {proposal.multiDay && (
              <View className="flex-row items-center gap-1">
                <MoonStar size={13} color="#FF9500" />
                <Text className="text-[13px] text-[#FF9500]">Multi-jour</Text>
              </View>
            )}
          </View>
        </View>

        <View className="flex-row items-center justify-between">
          <Text className="text-[28px] font-bold text-black tracking-tight">{formatTime(proposal.departureTime)}</Text>
          <View className="flex-1 mx-4 h-[1.5px] bg-gray-200" />
          <Text className="text-[28px] font-bold text-black tracking-tight">{formatTime(proposal.arrivalTime)}</Text>
        </View>

        {/* Route color bar */}
        <View className="flex-row rounded-full overflow-hidden h-[5px] mt-3">
          {proposal.legs.map((leg, i) => (
            <View
              key={i}
              className="h-full"
              style={{
                flex: 1,
                backgroundColor: leg.routeColor ? `#${leg.routeColor}` : "#007AFF",
                marginRight: i < proposal.legs.length - 1 ? 2 : 0,
                borderRadius: 8,
              }}
            />
          ))}
        </View>
      </View>

      {/* Timeline des legs */}
      <ScrollView className="flex-1" showsVerticalScrollIndicator={false}>
        {proposal.legs.map((leg, i) => {
          const routeColor = leg.routeColor ? `#${leg.routeColor}` : "#007AFF";

          return (
            <View key={i} className="mb-1">
              {/* Départ du leg */}
              <View className="flex-row">
                {/* Timeline colonne gauche */}
                <View className="w-12 items-center">
                  <View className="w-3.5 h-3.5 rounded-full border-[3px] bg-white z-10" style={{ borderColor: routeColor }} />
                  <View className="flex-1 w-[3px] rounded-full -mt-0.5" style={{ backgroundColor: routeColor }} />
                </View>

                {/* Contenu */}
                <View className="flex-1 pb-5 -mt-1">
                  <View className="flex-row items-center justify-between">
                    <Text className="text-[15px] font-semibold text-black">{leg.originStopName}</Text>
                    <Text className="text-[15px] font-bold text-black">{formatTime(leg.departureTime)}</Text>
                  </View>

                  {/* Badge de la ligne */}
                  <View className="flex-row items-center mt-2 gap-2">
                    <View className="flex-row items-center px-2.5 py-1 rounded-lg gap-1.5" style={{ backgroundColor: routeColor }}>
                      <Train size={12} color="white" />
                      <Text className="text-white text-[12px] font-bold">{leg.routeShortName || "—"}</Text>
                    </View>
                    {leg.routeLongName ?
                      <Text className="text-[13px] text-gray-500 flex-1" numberOfLines={1}>
                        {leg.routeLongName}
                      </Text>
                    : null}
                  </View>

                  {leg.headSign ?
                    <Text className="text-[13px] text-gray-400 mt-1">Direction {leg.headSign}</Text>
                  : null}

                  {/* Durée du leg */}
                  <View className="flex-row items-center mt-2 gap-1.5">
                    <Clock size={12} color="#8E8E93" />
                    <Text className="text-[12px] text-gray-400">{formatTravelTime(leg.travelTime.hours, leg.travelTime.minutes)}</Text>
                  </View>
                </View>
              </View>

              {/* Arrivée du leg (dernier arrêt) */}
              <View className="flex-row">
                <View className="w-12 items-center">
                  {i < proposal.legs.length - 1 ?
                    <>
                      <View className="w-3 h-3 rounded-full z-10" style={{ backgroundColor: routeColor }} />
                      {/* Ligne de correspondance pointillée */}
                      <View className="flex-1 w-[1.5px] border-l-[1.5px] border-dashed border-gray-300 my-0.5" />
                    </>
                  : <View className="w-3.5 h-3.5 rounded-full z-10" style={{ backgroundColor: routeColor }} />}
                </View>

                <View className={`flex-1 -mt-1 ${i < proposal.legs.length - 1 ? "pb-5" : "pb-2"}`}>
                  <View className="flex-row items-center justify-between">
                    <Text className="text-[15px] font-semibold text-black">{leg.destinationStopName}</Text>
                    <Text className="text-[15px] font-bold text-black">{formatTime(leg.arrivalTime)}</Text>
                  </View>

                  {i < proposal.legs.length - 1 && (
                    <View className="flex-row items-center mt-2 gap-1.5 bg-orange-50 self-start px-2.5 py-1 rounded-lg">
                      <Repeat size={12} color="#FF9500" />
                      <Text className="text-[12px] font-medium text-[#FF9500]">Correspondance</Text>
                    </View>
                  )}
                </View>
              </View>
            </View>
          );
        })}
      </ScrollView>

      <View className="flex-row gap-3 pt-3 pb-4">
        <TouchableOpacity
          onPress={onToggleSave}
          activeOpacity={0.7}
          className={`flex-1 flex-row items-center justify-center py-3 rounded-xl ${isSaved ? "bg-[#007AFF]" : "bg-white"}`}
          style={{
            shadowColor: "#000",
            shadowOffset: { width: 0, height: 1 },
            shadowOpacity: 0.06,
            shadowRadius: 10,
            elevation: 2,
          }}
        >
          {isSaved ?
            <BookmarkCheck size={16} color="white" />
          : <Bookmark size={16} color="#007AFF" />}
          <Text className={`font-semibold ml-1.5 text-[15px] ${isSaved ? "text-white" : "text-[#007AFF]"}`}>{isSaved ? "Sauvegardé" : "Sauvegarder"}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}
