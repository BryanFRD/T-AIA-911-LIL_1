import React, { useState, useEffect } from "react";
import { View, Text } from "react-native";
import { StatusBar } from "expo-status-bar";
import LocationPermissionModal from "../components/modal/LocationPermissionModal";
import { AnimatedTransition } from "../components/transitions";
import { Header, LocationDisplay, RecordingTimer, RecordButton, LoadingView, TranscriptionDisplay } from "../components/voiceRecorder";
import TravelResults from "../components/travel/TravelResults";
import { useVoiceRecorder } from "../hooks/useVoiceRecorder";
import { useLocation } from "../hooks/useLocation";
import { useTranscribeAudio } from "@/hooks/queries/useTranscription";
import { useSolution } from "@/hooks/queries/useSolution";
import { TranscriptionResponse } from "@/interfaces/transcription.interface";
import { SolutionResponse } from "@/interfaces/solution.interface";

export default function VoiceRecorderScreen() {
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [transcribedData, setTranscribedData] = useState<TranscriptionResponse | null>(null);
  const [solutionData, setSolutionData] = useState<SolutionResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const { isRecording, timer, startRecording, stopRecording, stopAndDeleteRecording } = useVoiceRecorder();
  const { address, coordinates, loadingLocation, permissionDenied, fadeAnim, slideAnim, requestLocation } = useLocation();

  const { mutate: transcribeAudio, error, isPending, isError } = useTranscribeAudio();
  const { mutate: findSolution, isPending: isSolutionPending, isError: isSolutionError, error: solutionError } = useSolution();

  // Synchroniser isLoading avec isPending de la mutation de transcription
  useEffect(() => {
    setIsLoading(isPending);
  }, [isPending]);

  const handleRecordButtonPress = async () => {
    if (isRecording) {
      const uri = await stopRecording();

      if (uri) {
        transcribeAudio(uri, {
          onSuccess: (data) => {
            setTranscribedData(data);
            console.log("Transcription:", data.transcription.transcribedText);
          },
          onError: (err) => {
            console.error("Failed to send audio for transcription:", err);
          },
        });
      }
    } else {
      await startRecording();
    }
  };

  const handleConfirm = () => {
    if (!transcribedData) return;
    findSolution(
      {
        customerPath: transcribedData.transcription.transcribedText,
        latitude: coordinates?.latitude ?? 0,
        longitude: coordinates?.longitude ?? 0,
      },
      {
        onSuccess: (data) => {
          setSolutionData(data);
        },
        onError: (err) => {
          console.error("Failed to find solution:", err);
        },
      },
    );
  };

  const handleCancel = () => {
    setTranscribedData(null);
    setSolutionData(null);
  };

  const handleNewRequest = () => {
    setTranscribedData(null);
    setSolutionData(null);
  };

  const handleSave = () => {
    // implémenter la sauvegarde
  };

  return (
    <View className="flex-1 bg-[#F2F2F7]">
      <StatusBar style="dark" />
      <LocationPermissionModal visible={showPermissionModal} onClose={() => setShowPermissionModal(false)} />

      <AnimatedTransition
        isLoading={isLoading}
        loadingContent={
          <View className="flex-1 pt-24">
            <LoadingView message="Transcription en cours" />
          </View>
        }
      >
        <View className="pt-20 pb-4 px-6">
          <Header title="Travel Order Resolver" subtitle="Assistant IA vocal" />
        </View>

        <View className="flex-1 px-5">
          {solutionData ?
            <TravelResults solution={solutionData} onSave={handleSave} onNewRequest={handleNewRequest} />
          : transcribedData ?
            <TranscriptionDisplay
              transcribedText={transcribedData.transcription.transcribedText}
              onConfirm={handleConfirm}
              onCancel={handleCancel}
              isLoadingSolution={isSolutionPending}
            />
          : <LocationDisplay
              address={address}
              permissionDenied={permissionDenied}
              loadingLocation={loadingLocation}
              fadeAnim={fadeAnim}
              slideAnim={slideAnim}
              onRequestLocation={requestLocation}
              onShowPermissionModal={() => setShowPermissionModal(true)}
            />
          }

          {isError && (
            <View className="mt-4 p-4 bg-red-50 rounded-2xl w-full">
              <Text className="text-sm font-semibold text-red-600 mb-1">Erreur {(error as any)?.response?.status || ""}</Text>
              <Text className="text-base text-red-500">{(error as any)?.response?.data?.message || error?.message || "Impossible de transcrire l'audio"}</Text>
            </View>
          )}

          {isSolutionError && (
            <View className="mt-4 p-4 bg-red-50 rounded-2xl w-full">
              <Text className="text-sm font-semibold text-red-600 mb-1">Erreur de recherche</Text>
              <Text className="text-base text-red-500">
                {(solutionError as any)?.response?.data?.message || solutionError?.message || "Impossible de trouver un itinéraire"}
              </Text>
            </View>
          )}
        </View>

        {!solutionData && (
          <View className="pb-10 pt-4">
            {isRecording && <RecordingTimer timer={timer} />}
            <RecordButton isRecording={isRecording} onSend={handleRecordButtonPress} onCancel={stopAndDeleteRecording} />
          </View>
        )}
      </AnimatedTransition>
    </View>
  );
}
