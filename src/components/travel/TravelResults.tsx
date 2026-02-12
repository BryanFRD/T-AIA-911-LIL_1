import React, { useState } from "react";
import { View, Text, ScrollView, TouchableOpacity } from "react-native";
import { Route, Bookmark, RotateCcw } from "lucide-react-native";
import { SolutionResponse, Proposal } from "@/interfaces/solution.interface";
import TravelProposalCard from "./TravelProposalCard";
import ProposalDetail from "./ProposalDetail";

interface TravelResultsProps {
  solution: SolutionResponse;
  onSave: () => void;
  onNewRequest: () => void;
}

export default function TravelResults({ solution, onSave, onNewRequest }: TravelResultsProps) {
  const [selectedProposal, setSelectedProposal] = useState<Proposal | null>(null);

  if (selectedProposal) {
    return <ProposalDetail proposal={selectedProposal} onBack={() => setSelectedProposal(null)} />;
  }

  return (
    <View className="flex-1">
      {/* Résumé de la recherche */}
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
        <View className="flex-row items-center gap-2 mb-2">
          <Route size={16} color="#007AFF" />
          <Text className="text-[13px] font-semibold text-[#007AFF] uppercase tracking-wider">Itinéraire trouvé</Text>
        </View>

        <View className="flex-row items-center">
          <View className="flex-1">
            <View className="flex-row items-center gap-2">
              <View className="w-2 h-2 rounded-full bg-[#007AFF]" />
              <Text className="text-[15px] font-semibold text-black" numberOfLines={1}>
                {solution.customerQuery.origin}
              </Text>
            </View>

            {solution.customerQuery.vias.length > 0 && (
              <View className="ml-3 my-1 border-l-[1.5px] border-dashed border-gray-200 pl-3 py-1">
                {solution.customerQuery.vias.map((via, i) => (
                  <View key={i} className="flex-row items-center gap-1.5 mb-0.5">
                    <View className="w-1.5 h-1.5 rounded-full bg-[#FF9500]" />
                    <Text className="text-[13px] text-gray-400">{via}</Text>
                  </View>
                ))}
              </View>
            )}

            <View className="flex-row items-center gap-2">
              <View className="w-2 h-2 rounded-full bg-[#34C759]" />
              <Text className="text-[15px] font-semibold text-black" numberOfLines={1}>
                {solution.customerQuery.destination}
              </Text>
            </View>
          </View>
        </View>
      </View>

      {/* Liste des propositions */}
      <Text className="text-[13px] font-semibold text-gray-400 uppercase tracking-wider mb-2 ml-1">
        {solution.proposals.length} {solution.proposals.length > 1 ? "propositions" : "proposition"}
      </Text>

      <ScrollView className="flex-1" showsVerticalScrollIndicator={false}>
        {solution.proposals.map((proposal, index) => (
          <TravelProposalCard key={index} proposal={proposal} index={index} onPress={() => setSelectedProposal(proposal)} />
        ))}
        <View className="h-4" />
      </ScrollView>

      {/* Bottom actions */}
      <View className="flex-row gap-3 pt-3 pb-10">
        <TouchableOpacity
          onPress={onSave}
          activeOpacity={0.7}
          className="flex-1 flex-row items-center justify-center py-3 bg-white rounded-xl"
          style={{
            shadowColor: "#000",
            shadowOffset: { width: 0, height: 1 },
            shadowOpacity: 0.06,
            shadowRadius: 10,
            elevation: 2,
          }}
        >
          <Bookmark size={16} color="#007AFF" />
          <Text className="text-[#007AFF] font-semibold ml-1.5 text-[15px]">Sauvegarder</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={onNewRequest} activeOpacity={0.7} className="flex-1 flex-row items-center justify-center py-3 bg-[#007AFF] rounded-xl">
          <RotateCcw size={16} color="white" />
          <Text className="text-white font-semibold ml-1.5 text-[15px]">Nouvelle recherche</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}
